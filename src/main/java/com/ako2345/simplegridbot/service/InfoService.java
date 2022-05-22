package com.ako2345.simplegridbot.service;

import com.ako2345.simplegridbot.model.CachedCandle;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;
import ru.tinkoff.piapi.core.stream.StreamProcessor;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InfoService {

    public static final String PRICE_STREAM = "PriceStream";

    private final SdkService sdkService;

    public boolean isInstrumentAvailableForTrading(String figi) {
        var instrument = sdkService.getInvestApi().getInstrumentsService().getInstrumentByFigiSync(figi);
        var name = instrument.getName();
        if (instrument.getTradingStatus() == SecurityTradingStatus.SECURITY_TRADING_STATUS_NOT_AVAILABLE_FOR_TRADING) {
            log.error("Instrument (figi: {}, name: {}) is not available for trading", figi, name);
            return false;
        }
        if (instrument.getApiTradeAvailableFlag()) {
            log.info("Instrument (figi: {}, name: {}) is available for trading via API", figi, name);
        } else {
            log.error("Instrument (figi: {}, name: {}) is not available for trading via API", figi, name);
            return false;
        }
        log.info("Instrument (figi: {}, name: {}) trading status: {}", figi, name, instrument.getTradingStatus());
        log.info("Instrument (figi: {}, name: {}) OTC flag: {}", figi, name, instrument.getOtcFlag());
        return true;
    }

    public BigDecimal getLastPrice(String figi) {
        var marketDataService = sdkService.getInvestApi().getMarketDataService();
        var lastPrices = marketDataService.getLastPricesSync(Collections.singletonList(figi));
        var lastPrice = lastPrices.get(0).getPrice();
        return MapperUtils.quotationToBigDecimal(lastPrice);
    }

    public void subscribePrice(String figi, StreamProcessor<MarketDataResponse> processor) {
        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

        var marketDataStreamService = sdkService.getInvestApi().getMarketDataStreamService();

        var stream = marketDataStreamService.getStreamById(PRICE_STREAM);
        if (stream == null) {
            stream = marketDataStreamService.newStream(PRICE_STREAM, processor, onErrorCallback);
        }

        stream.subscribeLastPrices(Collections.singletonList(figi));
    }

    public void unsubscribePrice(String figi) {
        sdkService.getInvestApi().getMarketDataStreamService().getStreamById(PRICE_STREAM).unsubscribeLastPrices(Collections.singletonList(figi));
    }

    @SneakyThrows
    public Set<CachedCandle> getCandles(String figi, Instant startTime, Instant endTime, CandleInterval candleInterval) {
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