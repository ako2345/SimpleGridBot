package com.ako2345.simplegridbot.bot;

import com.ako2345.simplegridbot.Constants;
import com.ako2345.simplegridbot.bot.grid.Grid;
import com.ako2345.simplegridbot.bot.grid.GridManager;
import com.ako2345.simplegridbot.controller.config.GridBotConfig;
import com.ako2345.simplegridbot.model.TransactionPair;
import com.ako2345.simplegridbot.order.OrderManager;
import com.ako2345.simplegridbot.order.OrderResult;
import com.ako2345.simplegridbot.util.TextUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

@Slf4j
public class GridBot extends AbsBot {

    private final GridManager gridManager;

    public GridBot(GridBotConfig config, BigDecimal initialPrice, OrderManager orderManager, BigDecimal lotSize) {
        super(config.figi, new BigDecimal(String.valueOf(config.investment)), BigDecimal.ZERO, initialPrice, lotSize, orderManager);
        var grid = new Grid(new BigDecimal(String.valueOf(config.lowerPrice)), new BigDecimal(String.valueOf(config.upperPrice)), config.gridsNumber);
        this.gridManager = new GridManager(grid, new BigDecimal(String.valueOf(config.investment)), initialPrice, lotSize);
        makeInitialOrder(initialPrice);
        log.info(
                "Grid bot created (lower price: {}, upper price: {}, grids number: {}, price step: {}, initial price: {}, lots per grid: {})",
                config.lowerPrice,
                config.upperPrice,
                config.gridsNumber,
                grid.getPriceStep().setScale(4, RoundingMode.HALF_DOWN),
                initialPrice.setScale(4, RoundingMode.HALF_DOWN),
                gridManager.getLotsPerGrid()
        );
        log.info("Price levels: {}", Arrays.toString(grid.getPriceLevels()));
    }

    @Override
    public void processPrice(BigDecimal price) {
        var timesToBuyOrSell = gridManager.processPrice(price);
        if (timesToBuyOrSell == 0) return;
        if (timesToBuyOrSell > 0) {
            if (Constants.LOG_SIGNALS) {
                log.info("Buy signal. Desired price: {}", price.setScale(4, RoundingMode.HALF_DOWN));
            }
            // Требуется покупка
            for (int i = 0; i < timesToBuyOrSell; i++) {
                buy(gridManager.getLotsPerGrid());
            }
        }
        if (timesToBuyOrSell < 0) {
            // Требуется продажа
            if (Constants.LOG_SIGNALS) {
                log.info("Sell signal. Desired price: {}", price.setScale(4, RoundingMode.HALF_DOWN));
            }
            for (int i = 0; i > timesToBuyOrSell; i--) {
                sell(gridManager.getLotsPerGrid());
            }
        }
    }

    public BigDecimal getGridProfit() {
        BigDecimal gridProfit = BigDecimal.ZERO;
        for (TransactionPair transactionPair : transactionPairs) {
            gridProfit = gridProfit.add(transactionPair.profit());
        }
        return gridProfit;
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

    private void makeInitialOrder(BigDecimal price) {
        var lotsToBuyOnStart = gridManager.lotsToBuyOnStart(price);
        if (lotsToBuyOnStart > 0) {
            log.info("Making initial order...");
            buy(lotsToBuyOnStart);
        }
    }

    @Override
    public void logOrderResults(String orderType, OrderResult orderResult) {
        log.info(
                "{} order (price: {}). Balance: {}, profit: {}, grid profit: {}, base currency amount: {}, instrument amount: {}",
                orderType,
                orderResult.getPrice().setScale(4, RoundingMode.HALF_DOWN),
                getBalance(orderResult.getPrice()).setScale(4, RoundingMode.HALF_DOWN),
                TextUtils.formatProfit(getProfit(orderResult.getPrice())),
                TextUtils.formatProfit(getGridProfit()),
                baseCurrencyAmount.setScale(4, RoundingMode.HALF_DOWN),
                instrumentAmount.setScale(4, RoundingMode.HALF_DOWN)
        );
    }
}