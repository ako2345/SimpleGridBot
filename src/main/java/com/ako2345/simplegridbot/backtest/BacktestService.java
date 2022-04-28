package com.ako2345.simplegridbot.backtest;

import com.ako2345.simplegridbot.assets.Assets;
import com.ako2345.simplegridbot.cache.InstrumentsCache;
import com.ako2345.simplegridbot.grid.Grid;
import com.ako2345.simplegridbot.grid.GridManager;
import com.ako2345.simplegridbot.model.CachedCandle;
import com.ako2345.simplegridbot.model.GridBotConfig;
import com.ako2345.simplegridbot.order.FakeOrderManager;
import com.ako2345.simplegridbot.order.OrderManager;
import com.ako2345.simplegridbot.service.SdkService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BacktestService {

    public static final int DAYS_TO_CHECK = 30 * 4;
    public static final CandleInterval CANDLE_INTERVAL = CandleInterval.CANDLE_INTERVAL_HOUR;

    private final SdkService sdkService;
    private final InstrumentsCache instrumentsCache;

    public String backtest(GridBotConfig config) {
        log.info("Starting backtest. Config: {}", config);

        // Загрузка данных об изменении цены
        var figi = config.figi;
        var endTime = OffsetDateTime.now().toInstant();
        var lotSize = instrumentsCache.getLotSize(figi);
        Set<CachedCandle> candles = new TreeSet<>(Comparator.comparingLong(candle -> candle.getTimestamp().getSeconds()));
        for (int i = 0; i < DAYS_TO_CHECK; i++) {
            var candlesForADay = getCandles(figi, endTime.minus(1, ChronoUnit.DAYS), endTime, CANDLE_INTERVAL);
            candles.addAll(candlesForADay);
            endTime = endTime.minus(1, ChronoUnit.DAYS);
        }
        log.info("Candles received. FIGI: {}, candleInterval: {}, size: {}", figi, CANDLE_INTERVAL, candles.size());

        // Проверка работы алгоритма
        var grid = new Grid(new BigDecimal(config.lowerPrice), new BigDecimal(config.upperPrice), config.gridsNumber);
        var initialPrice = candles.stream().findFirst().get().getOpen();
        var orderManager = new FakeOrderManager();
        var investment = new BigDecimal(config.investment);
        var gridManager = new GridManager(grid, investment, initialPrice, lotSize);
        var assets = new Assets(investment, BigDecimal.ZERO);

        init(initialPrice, gridManager, orderManager, assets, lotSize);

        final var priceStep = new BigDecimal("0.05");
        for (CachedCandle candle : candles) {
            // имитация изменения цены
            BigDecimal price = candle.getOpen();
            processPrice(price, gridManager, orderManager, assets, lotSize);
            while (true) {
                price = price.subtract(priceStep);
                if (price.compareTo(candle.getLow()) < 0) break;
                processPrice(price, gridManager, orderManager, assets, lotSize);
            }

            price = candle.getLow();
            processPrice(price, gridManager, orderManager, assets, lotSize);
            while (true) {
                price = price.add(priceStep);
                if (price.compareTo(candle.getHigh()) > 0) break;
                processPrice(price, gridManager, orderManager, assets, lotSize);
            }

            price = candle.getHigh();
            processPrice(price, gridManager, orderManager, assets, lotSize);
            while (true) {
                price = price.subtract(priceStep);
                if (price.compareTo(candle.getClose()) < 0) break;
                processPrice(price, gridManager, orderManager, assets, lotSize);
            }

            processPrice(candle.getClose(), gridManager, orderManager, assets, lotSize);
        }

        var finalPrice = candles.stream().reduce((prev, next) -> next).get().getClose();
        var finalBalance = assets.getBalance(finalPrice);
        var profit = finalBalance.divide(investment, 4, RoundingMode.DOWN);
        return "Profit: " + profit;
    }

    private void init(BigDecimal currentPrice, GridManager gridManager, OrderManager orderManager, Assets assets, BigDecimal lotSize) {
        var lotsToBuyOnStart = gridManager.lotsToBuyOnStart(currentPrice);
        var buyOrderResult = orderManager.buy(lotsToBuyOnStart, currentPrice);
        if (buyOrderResult.isSuccessful()) {
            assets.baseCurrencyAmount = assets.baseCurrencyAmount.subtract(buyOrderResult.getPrice().multiply(new BigDecimal(lotsToBuyOnStart)).multiply(lotSize));
            assets.instrumentAmount = assets.instrumentAmount.add(new BigDecimal(lotsToBuyOnStart).multiply(lotSize));
            log.info("Backtest. Initial order complete. Price: {}. Balance: {}. {}", currentPrice, assets.getBalance(currentPrice), assets);
        } else {
            log.error("Backtest. Failed to make initial order");
        }
    }

    public void processPrice(BigDecimal currentPrice, GridManager gridManager, OrderManager orderManager, Assets assets, BigDecimal lotSize) {
        var signal = gridManager.processPrice(currentPrice);
        switch (signal) {
            case BUY:
                var lotsToBuy = gridManager.getLotsPerGrid();
                var buyOrderResult = orderManager.buy(lotsToBuy, currentPrice);
                if (buyOrderResult.isSuccessful()) {
                    assets.baseCurrencyAmount = assets.baseCurrencyAmount.subtract(buyOrderResult.getPrice().multiply(new BigDecimal(lotsToBuy)).multiply(lotSize));
                    assets.instrumentAmount = assets.instrumentAmount.add(new BigDecimal(lotsToBuy).multiply(lotSize));
                    log.info("Backtest. Buy order complete. Price: {}. Balance: {}. {}", currentPrice, assets.getBalance(currentPrice), assets);
                } else {
                    log.error("Backtest. Failed to buy instrument");
                }
                break;
            case SELL:
                var lotsToSell = gridManager.getLotsPerGrid();
                var sellOrderResult = orderManager.sell(lotsToSell, currentPrice);
                if (sellOrderResult.isSuccessful()) {
                    assets.baseCurrencyAmount = assets.baseCurrencyAmount.add(sellOrderResult.getPrice().multiply(new BigDecimal(lotsToSell)).multiply(lotSize));
                    assets.instrumentAmount = assets.instrumentAmount.subtract(new BigDecimal(lotsToSell).multiply(lotSize));
                    log.info("Backtest. Sell order complete. Price: {}. Balance: {}. {}", currentPrice, assets.getBalance(currentPrice), assets);
                } else {
                    log.error("Backtest. Failed to sell instrument while");
                }
                break;
        }
    }

    @SneakyThrows
    private Set<CachedCandle> getCandles(String figi, Instant startTime, Instant endTime, CandleInterval candleInterval) {
        try {
            return sdkService.getInvestApi().getMarketDataService().getCandlesSync(figi, startTime, endTime, candleInterval)
                    .stream()
                    .map(CachedCandle::ofHistoricCandle)
                    .collect(Collectors.toSet());
        } catch (ApiRuntimeException exception) {
            Thread.sleep(1000);
            return getCandles(figi, startTime, endTime, candleInterval);
        }
    }

}
