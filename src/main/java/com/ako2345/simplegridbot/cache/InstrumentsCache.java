package com.ako2345.simplegridbot.cache;

import com.ako2345.simplegridbot.service.SdkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.Instrument;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class InstrumentsCache {

    private final Map<String, Instrument> instruments = new HashMap<>();

    private final SdkService sdkService;

    public void add(String figi) {
        var instrument = sdkService.getInvestApi().getInstrumentsService().getInstrumentByFigiSync(figi);
        instruments.put(figi, instrument);
    }

    public BigDecimal getLotSize(String figi) {
        if (!instruments.containsKey(figi)) {
            add(figi);
        }
        var lotSize = BigDecimal.valueOf(instruments.get(figi).getLot());
        if (lotSize.equals(BigDecimal.ZERO)) {
            throw new IllegalArgumentException("Lot size can not be 0. FIGI: " + figi);
        }
        return lotSize;
    }

}