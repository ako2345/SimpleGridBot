package com.ako2345.simplegridbot.controller;

import com.ako2345.simplegridbot.backtest.BacktestService;
import com.ako2345.simplegridbot.model.GridBotConfig;
import com.ako2345.simplegridbot.service.InfoService;
import com.ako2345.simplegridbot.service.SandboxAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ApplicationController {

    private final InfoService infoService;
    private final SandboxAccountService sandboxAccountService;
    private final BacktestService backtestService;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    @PostMapping("/grid_bot/init")
    public void initGridBot(@RequestBody GridBotConfig config) {
        // TODO
    }

    @PostMapping("/grid_bot/backtest")
    public ResponseEntity<String> backtest(@RequestBody GridBotConfig config) {
        var backtestResult = backtestService.backtest(config);
        return new ResponseEntity<>(backtestResult, HttpStatus.OK);
    }

    @GetMapping("/info")
    public void info(@RequestParam String figi) {
        var instrument = infoService.getInfo(figi);
        if (instrument != null) {
            log.info("Instrument info (" +
                            "name: {}, " +
                            "FIGI: {}, " +
                            "lot size: {}, " +
                            "trading status: {}, " +
                            "OTC flag: {}, " +
                            "API trade availability: {}" +
                            ")",
                    instrument.getName(),
                    instrument.getFigi(),
                    instrument.getLot(),
                    instrument.getTradingStatus().name(),
                    instrument.getOtcFlag(),
                    instrument.getApiTradeAvailableFlag()
            );
        } else {
            log.error("Error while getting instrument info!");
        }
    }

    @GetMapping("/subscribe_price")
    public void subscribePrice(@RequestParam String figi) {
        infoService.subscribePrice(figi);
    }

    @GetMapping("/portfolio")
    public void getFundsInfo() {
        sandboxAccountService.getFundsInfo();
    }

}