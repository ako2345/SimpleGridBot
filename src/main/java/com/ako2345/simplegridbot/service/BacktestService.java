package com.ako2345.simplegridbot.service;

import com.ako2345.simplegridbot.Constants;
import com.ako2345.simplegridbot.bot.GridBot;
import com.ako2345.simplegridbot.bot.GridBotStatistics;
import com.ako2345.simplegridbot.cache.InstrumentsCache;
import com.ako2345.simplegridbot.controller.config.BacktestConfig;
import com.ako2345.simplegridbot.model.CachedCandle;
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
        var orderManager = new FakeOrderManager(config.fee);
        FakeOrderManager.price = initialPrice;
        var gridBot = new GridBot(config.gridBotConfig, initialPrice, orderManager, lotSize);

        var gridBotStatistics = simulatePriceChanging(gridBot, candles);

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

    public GridBotStatistics simulatePriceChanging(GridBot gridBot, Set<CachedCandle> candles) {
        for (CachedCandle candle : candles) {
            // имитация изменения цены
            BigDecimal price = candle.getOpen();
            FakeOrderManager.price = price;
            gridBot.processPrice(price);
            while (true) {
                price = price.subtract(Constants.BACKTEST_PRICE_STEP);
                if (price.compareTo(candle.getLow()) < 0) break;
                FakeOrderManager.price = price;
                gridBot.processPrice(price);
            }

            price = candle.getLow();
            FakeOrderManager.price = price;
            gridBot.processPrice(price);
            while (true) {
                price = price.add(Constants.BACKTEST_PRICE_STEP);
                if (price.compareTo(candle.getHigh()) > 0) break;
                FakeOrderManager.price = price;
                gridBot.processPrice(price);
            }

            price = candle.getHigh();
            FakeOrderManager.price = price;
            gridBot.processPrice(price);
            while (true) {
                price = price.subtract(Constants.BACKTEST_PRICE_STEP);
                if (price.compareTo(candle.getClose()) < 0) break;
                FakeOrderManager.price = price;
                gridBot.processPrice(price);
            }

            price = candle.getClose();
            FakeOrderManager.price = price;
            gridBot.processPrice(price);
        }

        var finalPrice = candles.stream().reduce((prev, next) -> next).get().getClose();
        return gridBot.getStatistics(finalPrice);
    }

}