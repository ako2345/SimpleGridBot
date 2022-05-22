package com.ako2345.simplegridbot;

import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.math.BigDecimal;

public class Constants {

    public static final int DEFAULT_SCALE = 8;
    public static final BigDecimal BACKTEST_PRICE_STEP = new BigDecimal("0.05");
    public static final CandleInterval DEFAULT_CANDLE_INTERVAL = CandleInterval.CANDLE_INTERVAL_HOUR;

    public static final boolean LOG_NEW_PRICE = true;
    public static final boolean LOG_SIGNALS = false;
    public static final boolean LOG_ORDER_STREAM = true;

}