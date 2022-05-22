package com.ako2345.simplegridbot.bot;

import com.ako2345.simplegridbot.Constants;
import com.ako2345.simplegridbot.model.Direction;
import com.ako2345.simplegridbot.model.Transaction;
import com.ako2345.simplegridbot.model.TransactionPair;
import com.ako2345.simplegridbot.order.OrderManager;
import com.ako2345.simplegridbot.order.OrderResult;
import com.ako2345.simplegridbot.util.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbsBot {

    protected final String figi;
    protected final BigDecimal lotSize;
    protected final BigDecimal initialBalance;
    private final OrderManager orderManager;
    protected BigDecimal baseCurrencyAmount;
    protected BigDecimal instrumentAmount;
    protected List<TransactionPair> transactionPairs = new ArrayList<>();

    protected AbsBot(String figi, BigDecimal baseCurrencyAmount, BigDecimal instrumentAmount, BigDecimal initialPrice, BigDecimal lotSize, OrderManager orderManager) {
        if (!StringUtils.hasLength(figi))
            throw new IllegalArgumentException("Invalid FIGI");
        if (BigDecimal.ZERO.compareTo(baseCurrencyAmount) == 0 && BigDecimal.ZERO.compareTo(instrumentAmount) == 0)
            throw new IllegalArgumentException("Invalid investment");
        if (initialPrice.signum() <= 0)
            throw new IllegalArgumentException("Invalid initial price");
        if (lotSize.signum() <= 0)
            throw new IllegalArgumentException("Invalid lot size");

        this.figi = figi;
        this.baseCurrencyAmount = baseCurrencyAmount;
        this.instrumentAmount = instrumentAmount;
        this.initialBalance = getBalance(initialPrice);
        this.orderManager = orderManager;
        this.lotSize = lotSize;
    }

    abstract public void processPrice(BigDecimal price);

    public void buy(int lotsNumber) {
        var buyOrderResult = orderManager.buy(figi, lotsNumber, lotSize);
        if (buyOrderResult.isSuccessful()) {
            baseCurrencyAmount = baseCurrencyAmount.subtract(buyOrderResult.getBaseCurrencyAmount());
            instrumentAmount = instrumentAmount.add(BigDecimal.valueOf(lotsNumber).multiply(lotSize));

            var transaction = new Transaction(Direction.BUY, buyOrderResult.getPrice(), buyOrderResult.getBaseCurrencyAmount());
            var lastIncompleteTransactionPair = getLastIncompleteTransactionPair();
            if (lastIncompleteTransactionPair != null && lastIncompleteTransactionPair.getOpenTransaction().getDirection() == Direction.SELL) {
                lastIncompleteTransactionPair.setCloseTransaction(transaction);
            } else {
                transactionPairs.add(new TransactionPair(transaction));
            }

            if (Constants.LOG_ORDER_RESULTS) {
                logOrderResults("Buy", buyOrderResult);
            }
        } else {
            log.error("Failed to buy instrument");
        }
    }

    public void sell(int lotsNumber) {
        var sellOrderResult = orderManager.sell(figi, lotsNumber, lotSize);
        if (sellOrderResult.isSuccessful()) {
            baseCurrencyAmount = baseCurrencyAmount.add(sellOrderResult.getBaseCurrencyAmount());
            instrumentAmount = instrumentAmount.subtract(BigDecimal.valueOf(lotsNumber).multiply(lotSize));

            var transaction = new Transaction(Direction.SELL, sellOrderResult.getPrice(), sellOrderResult.getBaseCurrencyAmount());
            var lastIncompleteTransactionPair = getLastIncompleteTransactionPair();
            if (lastIncompleteTransactionPair != null && lastIncompleteTransactionPair.getOpenTransaction().getDirection() == Direction.BUY) {
                lastIncompleteTransactionPair.setCloseTransaction(transaction);
            } else {
                transactionPairs.add(new TransactionPair(transaction));
            }

            if (Constants.LOG_ORDER_RESULTS) {
                logOrderResults("Sell", sellOrderResult);
            }
        } else {
            log.error("Failed to sell instrument");
        }
    }

    public void logOrderResults(String orderType, OrderResult orderResult) {
        log.info(
                "{} order (price: {}). Balance: {}, profit: {}, base currency amount: {}, instrument amount: {}",
                orderType,
                orderResult.getPrice().setScale(4, RoundingMode.HALF_DOWN),
                getBalance(orderResult.getPrice()).setScale(4, RoundingMode.HALF_DOWN),
                TextUtils.formatProfit(getProfit(orderResult.getPrice())),
                baseCurrencyAmount.setScale(4, RoundingMode.HALF_DOWN),
                instrumentAmount.setScale(4, RoundingMode.HALF_DOWN)
        );
    }

    public void close(boolean isInstrumentShouldBeSold) {
        if (isInstrumentShouldBeSold) {
            var lotsToSell = instrumentAmount.divide(lotSize, Constants.DEFAULT_SCALE, RoundingMode.DOWN).intValue();
            sell(lotsToSell);
        }
        log.info("Grid bot closed");
    }

    public String getFigi() {
        return figi;
    }

    public BigDecimal getBalance(BigDecimal price) {
        return baseCurrencyAmount.add(instrumentAmount.multiply(price));
    }

    public BigDecimal getProfit(BigDecimal price) {
        var balance = getBalance(price);
        return balance.subtract(initialBalance);
    }

    public BigDecimal getProfitPercentage(BigDecimal price) {
        var profit = getProfit(price);
        return profit.divide(initialBalance, Constants.DEFAULT_SCALE, RoundingMode.DOWN);
    }

    public int getTransactionsCount() {
        var transactionsCount = 0;
        for (TransactionPair transactionPair : transactionPairs) {
            if (transactionPair.isIncomplete()) {
                transactionsCount++;
            } else {
                transactionsCount += 2;
            }
        }
        return transactionsCount;
    }

    private TransactionPair getLastIncompleteTransactionPair() {
        if (transactionPairs.isEmpty()) return null;
        for (int i = transactionPairs.size() - 1; i > 0; i--) {
            if (transactionPairs.get(i).isIncomplete()) return transactionPairs.get(i);
        }
        return null;
    }

}