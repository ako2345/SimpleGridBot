package com.ako2345.simplegridbot.model;

import com.google.protobuf.Timestamp;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.tinkoff.piapi.contract.v1.Candle;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;

@EqualsAndHashCode(of = {"timestamp"})
@Getter
public class CachedCandle {

    private final BigDecimal open;
    private final BigDecimal close;
    private final BigDecimal low;
    private final BigDecimal high;
    private final Timestamp timestamp;

    private CachedCandle(Timestamp timestamp, Quotation openPrice, Quotation closePrice, Quotation lowPrice, Quotation highPrice) {
        this.open = MapperUtils.quotationToBigDecimal(openPrice);
        this.low = MapperUtils.quotationToBigDecimal(lowPrice);
        this.high = MapperUtils.quotationToBigDecimal(highPrice);
        this.close = MapperUtils.quotationToBigDecimal(closePrice);
        this.timestamp = timestamp;
    }

    public static CachedCandle ofHistoricCandle(HistoricCandle candle) {
        return new CachedCandle(candle.getTime(), candle.getOpen(), candle.getClose(), candle.getLow(), candle.getHigh());
    }

    public static CachedCandle ofStreamCandle(Candle candle) {
        return new CachedCandle(candle.getTime(), candle.getOpen(), candle.getClose(), candle.getLow(), candle.getHigh());
    }

}