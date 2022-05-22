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
    private final InfoService infoService;
    private final RealAccountService realAccountService;
    private final SandboxAccountService sandboxAccountService;
    private final AnalysisService analysisService;
    private final BacktestService backtestService;
    private final InstrumentsCache instrumentsCache;
    private GridBot gridBot;

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
        var price = infoService.getLastPrice(config.figi);
        var orderManager = new TrueOrderManager(getAccountService());
        if (gridBot != null) {
            log.error("Current grid bot should be closed first");
            return;
        }
        if (!infoService.isInstrumentAvailableForTrading(config.figi)) {
            log.error("Grid bot cannot be created");
            return;
        }
        gridBot = new GridBot(config, price, orderManager, instrumentsCache.getLotSize(config.figi));
        StreamProcessor<MarketDataResponse> processor = response -> {
            var figi = response.getLastPrice().getFigi();
            if (response.hasLastPrice() && figi.equals(config.figi)) {
                var lastPriceQuotation = response.getLastPrice().getPrice();
                var lastPrice = MapperUtils.quotationToBigDecimal(lastPriceQuotation);

                if (Constants.LOG_NEW_PRICE) {
                    log.info("New price for FIGI {}: {}", figi, lastPrice.setScale(4, RoundingMode.DOWN));
                }

                gridBot.processPrice(lastPrice);
            }
        };
        infoService.subscribePrice(config.figi, processor);
    }

    @PostMapping("/grid_bot/close")
    public void closeGridBot(@RequestBody CloseGridBotParams params) {
        if (gridBot == null) {
            log.warn("Grid bot is not initialized");
            return;
        }
        infoService.unsubscribePrice(gridBot.getFigi());
        gridBot.close(params.isInstrumentShouldBeSold());
        gridBot = null;
    }

    private AccountService getAccountService() {
        return configService.getSandboxMode() ? sandboxAccountService : realAccountService;
    }

}