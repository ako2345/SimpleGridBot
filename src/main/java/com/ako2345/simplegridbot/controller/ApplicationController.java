package com.ako2345.simplegridbot.controller;

import com.ako2345.simplegridbot.Constants;
import com.ako2345.simplegridbot.bot.GridBot;
import com.ako2345.simplegridbot.cache.InstrumentsCache;
import com.ako2345.simplegridbot.cache.TradingScheduleCache;
import com.ako2345.simplegridbot.controller.config.AnalysisConfig;
import com.ako2345.simplegridbot.controller.config.BacktestConfig;
import com.ako2345.simplegridbot.controller.config.CloseGridBotParams;
import com.ako2345.simplegridbot.controller.config.GridBotConfig;
import com.ako2345.simplegridbot.order.TrueOrderManager;
import com.ako2345.simplegridbot.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.core.stream.StreamProcessor;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Timer;
import java.util.TimerTask;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ApplicationController {

    private final ConfigService configService;
    private final RealOrdersStreamService realOrdersStreamService;
    private final SandboxOrdersStreamService sandboxOrdersStreamService;
    private final InfoService infoService;
    private final RealOrderService realAccountService;
    private final SandboxOrderService sandboxAccountService;
    private final AnalysisService analysisService;
    private final BacktestService backtestService;
    private final InstrumentsCache instrumentsCache;
    private final TradingScheduleCache tradingScheduleCache;
    private GridBot gridBot;
    private Timer timer;

    @GetMapping("/")
    public ResponseEntity<String> statistics() {
        if (gridBot == null) {
            var noRunningBotsString = "There is no running bots at the moment";
            log.info(noRunningBotsString);
            return new ResponseEntity<>(noRunningBotsString, HttpStatus.OK);
        }
        var figi = gridBot.getFigi();
        var currentPrice = infoService.getLastPrice(figi);
        var gridBotStatistics = gridBot.getStatistics(currentPrice);
        var statisticsString = "Grid bot is active. " +
                "Instrument name: " + instrumentsCache.getName(figi) + " (FIGI: " + figi + "). " +
                "Statistics: " + gridBotStatistics.toString();
        log.info(statisticsString);
        return new ResponseEntity<>(statisticsString, HttpStatus.OK);
    }

    @PostMapping("/grid_bot/analyze")
    public void analyze(@RequestBody AnalysisConfig config) {
        analysisService.analyze(config);
    }

    @PostMapping("/grid_bot/backtest")
    public void backtest(@RequestBody BacktestConfig config) {
        backtestService.backtest(config);
    }

    @PostMapping("/grid_bot/init")
    public void initGridBot(@RequestBody GridBotConfig config) {
        var orderManager = new TrueOrderManager(getAccountService());

        // проверка активного бота
        if (gridBot != null) {
            log.error("Running grid bot should be closed first");
            return;
        }

        // проверка доступности тогрговли
        if (!infoService.isInstrumentAvailableForTrading(config.figi)) {
            log.error("Grid bot cannot be created");
            return;
        }
        gridBot = new GridBot(config, orderManager, instrumentsCache.getLotSize(config.figi), infoService.getLastPrice(config.figi));

        // подписка на информацию об ордерах
        if (configService.getSandboxMode()) {
            sandboxOrdersStreamService.addListener(gridBot);
        } else {
            realOrdersStreamService.addListener(gridBot);
        }

        // планирование обновления ордеров в следующей торговой сессии
        scheduleLimitOrdersUpdate(config.figi);

        // логирование изменения цены
        if (Constants.LOG_NEW_PRICE) {
            StreamProcessor<MarketDataResponse> processor = response -> {
                var figi = response.getLastPrice().getFigi();
                if (response.hasLastPrice() && figi.equals(config.figi)) {
                    var lastPriceQuotation = response.getLastPrice().getPrice();
                    var lastPrice = MapperUtils.quotationToBigDecimal(lastPriceQuotation);
                    log.info("Price for FIGI {}: {}", figi, lastPrice.setScale(4, RoundingMode.DOWN));
                }
            };
            infoService.subscribePrice(config.figi, processor);
        }
    }

    @PostMapping("/grid_bot/close")
    public void closeGridBot(@RequestBody CloseGridBotParams params) {
        // проверка активного бота
        if (gridBot == null) {
            log.warn("Grid bot is not initialized");
            return;
        }

        // отмена обновления ордеров в следующей торговой сессии
        timer.cancel();

        // отписка от информации о цене
        if (Constants.LOG_NEW_PRICE) {
            infoService.unsubscribePrice(gridBot.getFigi());
        }

        // отписка от информации об ордерах
        if (configService.getSandboxMode()) {
            sandboxOrdersStreamService.removeListener(gridBot);
        } else {
            realOrdersStreamService.removeListener(gridBot);
        }

        // закрытие бота
        gridBot.close(params.isInstrumentShouldBeSold());
        gridBot = null;
    }

    private void scheduleLimitOrdersUpdate(String figi) {
        var date = LocalDate.now();
        var exchange = instrumentsCache.getExchange(figi);
        while (true) {
            date = date.plusDays(1);
            var exchangeScheduleForDate = tradingScheduleCache.getExchangeScheduleForDate(exchange, date);
            if (exchangeScheduleForDate.isTradingDay()) {
                var nextTradeSessionOpenTime = exchangeScheduleForDate.getOpenTime();
                log.info("Scheduling orders update at {}...", nextTradeSessionOpenTime);
                var createLimitOrdersTask = new TimerTask() {
                    @Override
                    public void run() {
                        log.info("Creating limit orders for this trade session...");
                        if (gridBot != null) {
                            gridBot.clearPriceLevelsWithLimitOrders();
                            gridBot.createNewLimitOrders(infoService.getLastPrice(figi));
                            scheduleLimitOrdersUpdate(figi);
                        } else {
                            log.error("Grid bot is null");
                        }
                    }
                };
                timer = new Timer();
                timer.schedule(createLimitOrdersTask, Date.from(nextTradeSessionOpenTime));
                break;
            }
        }
    }

    private OrderService getAccountService() {
        return configService.getSandboxMode() ? sandboxAccountService : realAccountService;
    }

}