package com.ako2345.simplegridbot.order;

import java.math.BigDecimal;

public class FakeOrderManager implements OrderManager {

    public static BigDecimal price;

    private final float fee;

    public FakeOrderManager(float fee) {
        this.fee = fee;
    }

    @Override
    public OrderResult buy(String figi, int lotsNumber, BigDecimal lotSize) {
        var baseCurrencyAmount = price
                .multiply(BigDecimal.valueOf(lotsNumber * (1 + fee)))
                .multiply(lotSize);
        return new OrderResult(true, price, baseCurrencyAmount);
    }

    @Override
    public OrderResult sell(String figi, int lotsNumber, BigDecimal lotSize) {
        var baseCurrencyAmount = price
                .multiply(BigDecimal.valueOf(lotsNumber * (1 - fee)))
                .multiply(lotSize);
        return new OrderResult(true, price, baseCurrencyAmount);
    }

}