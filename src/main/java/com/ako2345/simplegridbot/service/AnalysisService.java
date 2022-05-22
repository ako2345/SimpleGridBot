package com.ako2345.simplegridbot.service;

import com.ako2345.simplegridbot.Constants;
import com.ako2345.simplegridbot.bot.GridBot;
import com.ako2345.simplegridbot.cache.InstrumentsCache;
import com.ako2345.simplegridbot.controller.config.AnalysisConfig;
import com.ako2345.simplegridbot.controller.config.GridBotConfig;
import com.ako2345.simplegridbot.model.CachedCandle;
import com.ako2345.simplegridbot.order.FakeOrderManager;
import com.ako2345.simplegridbot.util.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisService {

    private final InfoService infoService;
    private final BacktestService backtestService;
    private final InstrumentsCache instrumentsCache;

    public void analyze(AnalysisConfig config) {
        log.info("Starting analysis for {} days. FIGI: {}", config.days, config.figi);

        // Загрузка данных об изменении цены
        var endTime = OffsetDateTime.now().toInstant();
        var lotSize = instrumentsCache.getLotSize(config.figi);
        Set<CachedCandle> candles = new TreeSet<>(Comparator.comparingLong(candle -> candle.getTimestamp().getSeconds()));
        for (int i = 0; i < config.days; i++) {
            var candlesForADay = infoService.getCandles(
                    config.figi,
                    endTime.minus(1, ChronoUnit.DAYS),
                    endTime,
                    Constants.DEFAULT_CANDLE_INTERVAL
            );
            candles.addAll(candlesForADay);
            endTime = endTime.minus(1, ChronoUnit.DAYS);
        }
        log.info("Candles received (FIGI: {}, candleInterval: {}, size: {})", config.figi, Constants.DEFAULT_CANDLE_INTERVAL, candles.size());

        // Анализ данных
        var initialPrice = candles.stream().findFirst().get().getOpen();
        var finalPrice = candles.stream().reduce((prev, next) -> next).get().getClose();
        var minPrice = candles.stream().min(Comparator.comparing(CachedCandle::getLow)).get().getLow().floatValue();
        var maxPrice = candles.stream().max(Comparator.comparing(CachedCandle::getHigh)).get().getHigh().floatValue();
        var fakeOrderManager = new FakeOrderManager();

        var simulationResults = new HashMap<GridBotConfig, BigDecimal>();
        for (float lowerPrice = minPrice; lowerPrice < minPrice + (maxPrice - minPrice) / 2; lowerPrice += (maxPrice - minPrice) / 16) {
            for (float upperPrice = maxPrice - (maxPrice - minPrice) / 2; upperPrice <= maxPrice; upperPrice += (maxPrice - minPrice) / 16) {
                for (int gridsNumber = 2; gridsNumber < 50; gridsNumber++) {
                    var gridBotConfig = new GridBotConfig(config.figi, lowerPrice, upperPrice, gridsNumber, 10000000);
                    fakeOrderManager.setSimulatedPrice(initialPrice);
                    var gridBot = new GridBot(gridBotConfig, fakeOrderManager, lotSize, initialPrice);
                    var gridBotStatistics = backtestService.simulatePriceChanging(gridBot, fakeOrderManager, initialPrice, candles);
                    simulationResults.put(gridBotConfig, gridBotStatistics.getTotalProfitPercentage());
                }
            }
        }
        log.info(
                "Analysis for FIGI {} complete. Initial price: {}, final price: {}. Min price: {}, max price: {}. Best results:",
                config.figi,
                initialPrice.setScale(2, RoundingMode.DOWN),
                finalPrice.setScale(2, RoundingMode.DOWN),
                minPrice,
                maxPrice
        );
        simulationResults
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(5)
                .forEach(entry -> log.info(
                        "Total profit: {} (lower price: {}, upper price: {}, grids number: {})",
                        TextUtils.formatProfitPercentage(entry.getValue()),
                        entry.getKey().lowerPrice,
                        entry.getKey().upperPrice,
                        entry.getKey().gridsNumber
                ));
    }

}