package com.ako2345.simplegridbot.order;

import com.ako2345.simplegridbot.model.Order;

import java.math.BigDecimal;

public interface OrderManager {

    Order makeBuyMarketOrder(String figi, int lotsNumber, BigDecimal lotSize);

    Order makeSellMarketOrder(String figi, int lotsNumber, BigDecimal lotSize);

    Order makeBuyLimitOrder(String figi, int lotsNumber, BigDecimal lotSize, BigDecimal price);

    Order makeSellLimitOrder(String figi, int lotsNumber, BigDecimal lotSize, BigDecimal price);

    void cancelOrders(String figi);

}