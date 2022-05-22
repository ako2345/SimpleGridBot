package com.ako2345.simplegridbot.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Order {

    private final String orderId;
    private final String figi;
    private final BigDecimal price;
    private final Direction direction;
    private final long lotsNumber;
    private final BigDecimal baseCurrencyAmount;
    private final OrderStatus orderStatus;

}