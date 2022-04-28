package com.ako2345.simplegridbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import ru.tinkoff.piapi.contract.v1.Instrument;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.tinkoff.piapi.contract.v1.SubscriptionStatus;
import ru.tinkoff.piapi.core.stream.StreamProcessor;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.util.Collections;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class InfoService {

    private final SdkService sdkService;

    public Instrument getInfo(String figi) {
        try {
            return sdkService.getInvestApi().getInstrumentsService().getInstrumentByFigiSync(figi);
        } catch (Exception ignore) {
            return null;
        }
    }

    public void subscribePrice(@RequestParam String figi) {
        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());
        StreamProcessor<MarketDataResponse> processor = response -> {
            if (response.hasCandle()) {
                var candleFigi = response.getCandle().getFigi();
                var candlePrice = response.getCandle().getClose();
                log.info("New price for FIGI {}: {}", candleFigi, MapperUtils.quotationToBigDecimal(candlePrice));
            } else if (response.hasSubscribeCandlesResponse()) {
                var successCount = response.getSubscribeCandlesResponse()
                        .getCandlesSubscriptionsList()
                        .stream()
                        .filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS))
                        .count();
                var errorCount = response.getSubscribeTradesResponse()
                        .getTradeSubscriptionsList()
                        .stream()
                        .filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS))
                        .count();
                log.info("Successful candles subscriptions: {}", successCount);
                log.info("Unsuccessful candles subscriptions: {}", errorCount);
            }
        };

        var marketDataStreamService = sdkService.getInvestApi().getMarketDataStreamService();

        var streamName = "DefaultStream";
        var stream = marketDataStreamService.getStreamById(streamName);
        if (stream == null) {
            stream = marketDataStreamService.newStream(streamName, processor, onErrorCallback);
        }

        stream.subscribeCandles(Collections.singletonList(figi), SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE);
    }

}