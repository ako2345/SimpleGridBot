package com.ako2345.simplegridbot.order;

import java.math.BigDecimal;

public class FakeOrderManager implements OrderManager {

    @Override
    public OrderResult buy(int lotsPerGrid, BigDecimal currentPrice) {
        return new OrderResult(true, currentPrice);
    }

    @Override
    public OrderResult sell(int lotsPerGrid, BigDecimal currentPrice) {
        return new OrderResult(true, currentPrice);
    }

}