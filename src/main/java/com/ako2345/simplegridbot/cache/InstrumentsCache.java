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

    private Instrument getInstrument(String figi) {
        var instrument = instruments.get(figi);
        if (instrument == null) {
            instrument = sdkService.getInvestApi().getInstrumentsService().getInstrumentByFigiSync(figi);
            instruments.put(figi, instrument);
        }
        return instrument;
    }

    public String getName(String figi) {
        var instrument = getInstrument(figi);
        return instrument.getName();
    }

    public String getExchange(String figi) {
        var instrument = getInstrument(figi);
        return instrument.getExchange();
    }

    public BigDecimal getLotSize(String figi) {
        var instrument = getInstrument(figi);
        var lotSize = BigDecimal.valueOf(instrument.getLot());
        if (BigDecimal.ZERO.compareTo(lotSize) == 0) {
            throw new IllegalArgumentException("Lot size can not be 0. FIGI: " + figi);
        }
        return lotSize;
    }

}