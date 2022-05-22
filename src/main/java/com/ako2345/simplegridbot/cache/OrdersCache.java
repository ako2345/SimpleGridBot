package com.ako2345.simplegridbot.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OrdersCache {

    private final Map<String, Long> executedLotsNumberMap = new HashMap<>();
    private final Map<String, BigDecimal> baseCurrencyAmountMap = new HashMap<>();

    public long getExecutedLotsNumber(String orderId) {
        return executedLotsNumberMap.get(orderId);
    }

    public BigDecimal getBaseCurrencyAmount(String orderId) {
        return baseCurrencyAmountMap.get(orderId);
    }

    public void addExecutedLotsNumber(String orderId, long executedLotsNumber) {
        var previousExecutedLotsNumber = executedLotsNumberMap.get(orderId);
        var newExecutedLotsNumber = previousExecutedLotsNumber != null ? previousExecutedLotsNumber + executedLotsNumber : executedLotsNumber;
        executedLotsNumberMap.put(orderId, newExecutedLotsNumber);
    }

    public void addBaseCurrencyAmount(String orderId, BigDecimal baseCurrencyAmount) {
        var previousBaseCurrencyAmount = baseCurrencyAmountMap.get(orderId);
        var newBaseCurrencyAmount = previousBaseCurrencyAmount != null ? previousBaseCurrencyAmount.add(baseCurrencyAmount) : baseCurrencyAmount;
        baseCurrencyAmountMap.put(orderId, newBaseCurrencyAmount);
    }

}