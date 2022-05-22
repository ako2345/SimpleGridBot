package com.ako2345.simplegridbot.bot;

import com.ako2345.simplegridbot.Constants;
import com.ako2345.simplegridbot.bot.grid.Grid;
import com.ako2345.simplegridbot.bot.grid.GridManager;
import com.ako2345.simplegridbot.controller.config.GridBotConfig;
import com.ako2345.simplegridbot.model.Direction;
import com.ako2345.simplegridbot.model.OrderStatus;
import com.ako2345.simplegridbot.model.Transaction;
import com.ako2345.simplegridbot.model.TransactionPair;
import com.ako2345.simplegridbot.order.OrderManager;
import com.ako2345.simplegridbot.service.OrdersStreamServiceListener;
import com.ako2345.simplegridbot.util.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class GridBot implements OrdersStreamServiceListener {

    protected final String figi;
    protected final BigDecimal lotSize;
    protected final BigDecimal initialBalance;
    private final OrderManager orderManager;
    private final GridManager gridManager;
    protected BigDecimal baseCurrencyAmount;
    protected BigDecimal instrumentAmount;
    protected BigDecimal activePriceLevel = null;
    protected List<BigDecimal> pricesWithLimitOrders = new ArrayList<>();
    protected List<TransactionPair> transactionPairs = new ArrayList<>();
    private boolean isInitializing = true;
    private boolean isClosing = false;

    public GridBot(GridBotConfig config, OrderManager orderManager, BigDecimal lotSize, BigDecimal initialPrice) {
        if (!StringUtils.hasLength(config.figi))
            throw new IllegalArgumentException("Invalid FIGI");
        if (config.investment == 0F)
            throw new IllegalArgumentException("Invalid investment");
        if (lotSize.signum() <= 0)
            throw new IllegalArgumentException("Invalid lot size");
        if (initialPrice.signum() <= 0)
            throw new IllegalArgumentException("Invalid price");

        this.figi = config.figi;
        this.baseCurrencyAmount = BigDecimal.valueOf(config.investment);
        this.instrumentAmount = BigDecimal.ZERO;
        this.initialBalance = this.baseCurrencyAmount;
        this.orderManager = orderManager;
        this.lotSize = lotSize;
        var grid = new Grid(new BigDecimal(String.valueOf(config.lowerPrice)), new BigDecimal(String.valueOf(config.upperPrice)), config.gridsNumber);
        this.gridManager = new GridManager(grid, BigDecimal.valueOf(config.investment), lotSize);

        makeInitialBuyOrder(initialPrice);

        isInitializing = false;

        createNewLimitOrders(initialPrice);

        log.info(
                "Grid bot created (lower price: {}, upper price: {}, grids number: {}, price step: {}, lots per grid: {})",
                config.lowerPrice,
                config.upperPrice,
                config.gridsNumber,
                grid.getPriceStep().setScale(4, RoundingMode.HALF_DOWN),
                gridManager.getLotsPerGrid()
        );
        log.info("Price levels: {}", Arrays.toString(grid.getPriceLevels()));
    }

    public BigDecimal getBalance(BigDecimal price) {
        return baseCurrencyAmount.add(instrumentAmount.multiply(price));
    }

    public BigDecimal getProfit(BigDecimal price) {
        var balance = getBalance(price);
        return balance.subtract(initialBalance);
    }

    public BigDecimal getGridProfit() {
        BigDecimal gridProfit = BigDecimal.ZERO;
        for (TransactionPair transactionPair : transactionPairs) {
            gridProfit = gridProfit.add(transactionPair.profit());
        }
        return gridProfit;
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

    public GridBotStatistics getStatistics(BigDecimal price) {
        var totalProfit = getProfit(price);
        var gridProfit = getGridProfit();
        var unrealizedProfit = getBalance(price).subtract(gridProfit).subtract(initialBalance);
        var totalProfitPercentage = totalProfit.divide(initialBalance, Constants.DEFAULT_SCALE, RoundingMode.DOWN);
        var gridProfitPercentage = gridProfit.divide(initialBalance, Constants.DEFAULT_SCALE, RoundingMode.DOWN);
        var unrealizedProfitPercentage = unrealizedProfit.divide(initialBalance, Constants.DEFAULT_SCALE, RoundingMode.DOWN);
        var transactionsCount = getTransactionsCount();
        return new GridBotStatistics(
                totalProfitPercentage,
                totalProfit,
                gridProfitPercentage,
                gridProfit,
                unrealizedProfitPercentage,
                unrealizedProfit,
                transactionsCount
        );
    }

    private void makeInitialBuyOrder(BigDecimal currentPrice) {
        var lotsToBuyOnStart = gridManager.lotsToBuyOnStart(currentPrice);
        if (lotsToBuyOnStart > 0) {
            log.info("Making initial order (initial price: {})...", currentPrice.setScale(4, RoundingMode.HALF_DOWN));
            var order = orderManager.makeBuyMarketOrder(figi, lotsToBuyOnStart, lotSize);
            if (order.getOrderStatus() == OrderStatus.FILL) {
                processOrder(
                        order.getFigi(),
                        order.getDirection(),
                        order.getPrice(),
                        order.getBaseCurrencyAmount(),
                        order.getLotsNumber()
                );
            } else {
                log.error("Unexpected post order status: {}", order.getOrderStatus());
            }
        }
    }

    public void close(boolean isInstrumentShouldBeSold) {
        orderManager.cancelOrders(figi);
        isClosing = true;
        if (isInstrumentShouldBeSold && instrumentAmount.compareTo(BigDecimal.ZERO) > 0) {
            var lotsToSell = instrumentAmount.divide(lotSize, Constants.DEFAULT_SCALE, RoundingMode.DOWN).intValue();
            log.info("Selling {} lots...", lotsToSell);
            var order = orderManager.makeSellMarketOrder(figi, lotsToSell, lotSize);
            if (order.getOrderStatus() == OrderStatus.FILL) {
                processOrder(
                        order.getFigi(),
                        order.getDirection(),
                        order.getPrice(),
                        order.getBaseCurrencyAmount(),
                        order.getLotsNumber()
                );
            } else {
                log.error("Unexpected post order status: {}", order.getOrderStatus());
            }
        }
        log.info(
                "Grid profit: {}, base currency amount: {}, instrument amount: {}",
                TextUtils.formatProfit(getGridProfit()),
                this.baseCurrencyAmount.setScale(4, RoundingMode.DOWN),
                this.instrumentAmount.setScale(4, RoundingMode.DOWN)
        );
    }

    @Override
    public void processOrder(String orderFigi, Direction direction, BigDecimal price, BigDecimal baseCurrencyAmount, long lotsNumber) {
        if (!figi.equals(orderFigi)) return;

        activePriceLevel = price;
        pricesWithLimitOrders.remove(price);
        BigDecimal instrumentAmount = lotSize.multiply(BigDecimal.valueOf(lotsNumber));
        if (direction == Direction.BUY) {
            this.baseCurrencyAmount = this.baseCurrencyAmount.subtract(baseCurrencyAmount);
            this.instrumentAmount = this.instrumentAmount.add(instrumentAmount);
        } else {
            this.baseCurrencyAmount = this.baseCurrencyAmount.add(baseCurrencyAmount);
            this.instrumentAmount = this.instrumentAmount.subtract(instrumentAmount);
        }

        addTransaction(direction, price, baseCurrencyAmount);

        createNewLimitOrders(price);

        log.info(
                "{} order processed (price: {}). Balance: {}, profit: {}, grid profit: {}, base currency amount: {}, instrument amount: {}",
                direction,
                price.setScale(4, RoundingMode.DOWN),
                getBalance(price).setScale(4, RoundingMode.DOWN),
                TextUtils.formatProfit(getProfit(price)),
                TextUtils.formatProfit(getGridProfit()),
                this.baseCurrencyAmount.setScale(4, RoundingMode.DOWN),
                this.instrumentAmount.setScale(4, RoundingMode.DOWN)
        );
    }

    private void addTransaction(Direction direction, BigDecimal averageOrderPrice, BigDecimal baseCurrencyAmount) {
        var transaction = new Transaction(direction, averageOrderPrice, baseCurrencyAmount);
        if (isInitializing || isClosing) {
            transactionPairs.add(new TransactionPair(transaction));
        } else {
            var lastIncompleteTransactionPair = getLastIncompleteTransactionPair();
            if (lastIncompleteTransactionPair != null && lastIncompleteTransactionPair.getOpenTransaction().getDirection() != direction) {
                lastIncompleteTransactionPair.setCloseTransaction(transaction);
            } else {
                transactionPairs.add(new TransactionPair(transaction));
            }
        }
    }

    private void createNewLimitOrders(BigDecimal currentPrice) {
        var priceLevels = gridManager.getGrid().getPriceLevels();
        for (BigDecimal price : priceLevels) {
            if (pricesWithLimitOrders.contains(price)) continue;
            if (activePriceLevel != null && price.compareTo(activePriceLevel) == 0) continue;
            if (price.compareTo(currentPrice) < 0) {
                orderManager.makeBuyLimitOrder(figi, gridManager.getLotsPerGrid(), lotSize, price);
            } else {
                orderManager.makeSellLimitOrder(figi, gridManager.getLotsPerGrid(), lotSize, price);
            }
            pricesWithLimitOrders.add(price);
        }
    }

    public String getFigi() {
        return figi;
    }

    public GridManager getGridManager() {
        return gridManager;
    }

}