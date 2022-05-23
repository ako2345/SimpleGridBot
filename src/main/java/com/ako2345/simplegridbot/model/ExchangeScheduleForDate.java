package com.ako2345.simplegridbot.model;

import lombok.Data;

import java.time.Instant;

@Data
public class ExchangeScheduleForDate {

    private final boolean isTradingDay;
    private final Instant openTime;
    private final Instant closeTime;

}