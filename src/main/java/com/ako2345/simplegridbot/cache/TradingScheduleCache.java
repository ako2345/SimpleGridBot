package com.ako2345.simplegridbot.cache;

import com.ako2345.simplegridbot.model.ExchangeScheduleForDay;
import com.ako2345.simplegridbot.service.SdkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradingScheduleCache {

    private final SdkService sdkService;

    private final Map<String, Map<LocalDate, ExchangeScheduleForDay>> schedule = new HashMap<>();

    public ExchangeScheduleForDay getExchangeScheduleForDate(String exchange, LocalDate date) {
        var exchangeSchedule = schedule.computeIfAbsent(exchange, k -> new HashMap<>());
        var exchangeScheduleForDate = exchangeSchedule.get(date);
        if (exchangeScheduleForDate == null) {
            var tradingSchedule = sdkService.getInvestApi().getInstrumentsService().getTradingScheduleSync(
                    exchange,
                    date.atStartOfDay().toInstant(ZoneOffset.UTC),
                    date.atStartOfDay().toInstant(ZoneOffset.UTC)
            );
            if (tradingSchedule.getDaysCount() != 1) {
                throw new RuntimeException("Unexpected trading schedule response (days count: " + tradingSchedule.getDaysCount() + ")");
            }
            var tradingDay = tradingSchedule.getDaysList().get(0);
            if (tradingDay.getIsTradingDay()) {
                var startTime = Instant.ofEpochSecond(tradingDay.getStartTime().getSeconds());
                var closeTime = Instant.ofEpochSecond(tradingDay.getEndTime().getSeconds());
                exchangeScheduleForDate = new ExchangeScheduleForDay(true, startTime, closeTime);
            } else {
                exchangeScheduleForDate = new ExchangeScheduleForDay(false, null, null);
            }
            exchangeSchedule.put(date, exchangeScheduleForDate);
        }
        return exchangeScheduleForDate;
    }

}