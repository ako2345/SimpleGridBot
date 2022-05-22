package com.ako2345.simplegridbot.service;

import com.ako2345.simplegridbot.Constants;
import com.ako2345.simplegridbot.bot.GridBot;
import com.ako2345.simplegridbot.bot.GridBotStatistics;
import com.ako2345.simplegridbot.cache.InstrumentsCache;
import com.ako2345.simplegridbot.controller.config.BacktestConfig;
import com.ako2345.simplegridbot.model.CachedCandle;
import com.ako2345.simplegridbot.model.Order;
import com.ako2345.simplegridbot.order.FakeOrderManager;
import com.ako2345.simplegridbot.util.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

@Service
@Slf4j
@RequiredArgsConstructor
public class BacktestService {


    private final InfoService infoService;
    private final InstrumentsCache instrumentsCache;
    private BigDecimal processedPrice;

    public void backtest(BacktestConfig config) {
        log.info("Starting backtest for {} days. Bot config: {}", config.days, config.gridBotConfig);

        // Загрузка данных об изменении цены
        var figi = config.gridBotConfig.figi;
        var endTime = OffsetDateTime.now().toInstant();
        var lotSize = instrumentsCache.getLotSize(figi);
        Set<CachedCandle> candles = new TreeSet<>(Comparator.comparingLong(candle -> candle.getTimestamp().getSeconds()));
        for (int i = 0; i < config.days; i++) {
            var candlesForADay = infoService.getCandles(
                    figi,
                    endTime.minus(1, ChronoUnit.DAYS),
                    endTime,
                    Constants.DEFAULT_CANDLE_INTERVAL
            );
            candles.addAll(candlesForADay);
            endTime = endTime.minus(1, ChronoUnit.DAYS);
        }
        log.info("Candles received (FIGI: {}, candleInterval: {}, size: {})", figi, Constants.DEFAULT_CANDLE_INTERVAL, candles.size());

        // Проверка работы алгоритма
        var initialPrice = candles.stream().findFirst().get().getOpen();
        var fakeOrderManager = new FakeOrderManager();
        fakeOrderManager.setSimulatedPrice(initialPrice);
        var gridBot = new GridBot(config.gridBotConfig, fakeOrderManager, lotSize, initialPrice);

        var gridBotStatistics = simulatePriceChanging(gridBot, fakeOrderManager, initialPrice, candles);

        var totalProfitPercentageString = TextUtils.formatProfitPercentage(gridBotStatistics.getTotalProfitPercentage());
        var totalProfitString = TextUtils.formatProfit(gridBotStatistics.getTotalProfit());
        var gridProfitString = TextUtils.formatProfit(gridBotStatistics.getGridProfit());
        var unrealizedProfitString = TextUtils.formatProfit(gridBotStatistics.getUnrealizedProfit());
        log.info(
                "Backtest complete. Profit percentage: {}, total profit: {} (grid profit: {}, unrealized profit: {})",
                totalProfitPercentageString,
                totalProfitString,
                gridProfitString,
                unrealizedProfitString
        );
    }

    public GridBotStatistics simulatePriceChanging(GridBot gridBot, FakeOrderManager fakeOrderManager, BigDecimal initialPrice, Set<CachedCandle> candles) {
        processedPrice = initialPrice;
        for (CachedCandle candle : candles) {
            // имитация изменения цены
            BigDecimal price = candle.getOpen();
            processPrice(gridBot, fakeOrderManager, price);
            while (true) {
                price = price.subtract(Constants.BACKTEST_PRICE_STEP);
                if (price.compareTo(candle.getLow()) < 0) break;
                processPrice(gridBot, fakeOrderManager, price);
            }

            price = candle.getLow();
            processPrice(gridBot, fakeOrderManager, price);
            while (true) {
                price = price.add(Constants.BACKTEST_PRICE_STEP);
                if (price.compareTo(candle.getHigh()) > 0) break;
                processPrice(gridBot, fakeOrderManager, price);
            }

            price = candle.getHigh();
            processPrice(gridBot, fakeOrderManager, price);
            while (true) {
                price = price.subtract(Constants.BACKTEST_PRICE_STEP);
                if (price.compareTo(candle.getClose()) < 0) break;
                processPrice(gridBot, fakeOrderManager, price);
            }

            price = candle.getClose();
            processPrice(gridBot, fakeOrderManager, price);
        }

        var finalPrice = candles.stream().reduce((prev, next) -> next).get().getClose();
        return gridBot.getStatistics(finalPrice);
    }

    private void processPrice(GridBot gridBot, FakeOrderManager fakeOrderManager, BigDecimal price) {
        fakeOrderManager.setSimulatedPrice(price);
        var ordersToExecute = fakeOrderManager.getOrdersToExecute(price, processedPrice, gridBot.getGridManager().getGrid());
        if (!ordersToExecute.isEmpty()) {
            for (Order orderToExecute : ordersToExecute) {
                var figi = orderToExecute.getFigi();
                var orderPrice = orderToExecute.getPrice();
                var lotSize = instrumentsCache.getLotSize(figi);
                var lotsNumber = orderToExecute.getLotsNumber();
                var direction = orderToExecute.getDirection();
                var baseCurrencyAmount = price.multiply(lotSize).multiply(BigDecimal.valueOf(lotsNumber));
                gridBot.processOrder(
                        figi,
                        direction,
                        orderPrice,
                        baseCurrencyAmount,
                        lotsNumber
                );
            }
        }
        processedPrice = price;
    }

}