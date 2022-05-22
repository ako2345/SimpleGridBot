package com.ako2345.simplegridbot.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Transaction {

    private final Direction direction;
    private final BigDecimal price;
    private final BigDecimal baseCurrencyAmount;

}