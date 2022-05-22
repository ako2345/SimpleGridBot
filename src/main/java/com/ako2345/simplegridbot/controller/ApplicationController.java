package com.ako2345.simplegridbot.controller;

import com.ako2345.simplegridbot.Constants;
import com.ako2345.simplegridbot.bot.GridBot;
import com.ako2345.simplegridbot.cache.InstrumentsCache;
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
    private GridBot gridBot;

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
        if (gridBot != null) {
            log.error("Running grid bot should be closed first");
            return;
        }
        if (!infoService.isInstrumentAvailableForTrading(config.figi)) {
            log.error("Grid bot cannot be created");
            return;
        }
        gridBot = new GridBot(config, orderManager, instrumentsCache.getLotSize(config.figi), infoService.getLastPrice(config.figi));
        if (configService.getSandboxMode()) {
            sandboxOrdersStreamService.addListener(gridBot);
        } else {
            realOrdersStreamService.addListener(gridBot);
        }

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
        if (gridBot == null) {
            log.warn("Grid bot is not initialized");
            return;
        }
        if (Constants.LOG_NEW_PRICE) {
            infoService.unsubscribePrice(gridBot.getFigi());
        }
        if (configService.getSandboxMode()) {
            sandboxOrdersStreamService.removeListener(gridBot);
        } else {
            realOrdersStreamService.removeListener(gridBot);
        }
        gridBot.close(params.isInstrumentShouldBeSold());
        gridBot = null;
    }

    private OrderService getAccountService() {
        return configService.getSandboxMode() ? sandboxAccountService : realAccountService;
    }

}