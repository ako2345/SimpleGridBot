package com.ako2345.simplegridbot.service;

import com.ako2345.simplegridbot.model.Direction;

import java.math.BigDecimal;

public interface OrdersStreamServiceListener {

    void processOrder(String figi, Direction direction, BigDecimal averageOrderPrice, BigDecimal baseCurrencyAmount, long lotsNumber);

}