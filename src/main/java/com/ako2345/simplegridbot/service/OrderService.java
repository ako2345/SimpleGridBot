package com.ako2345.simplegridbot.service;

import com.ako2345.simplegridbot.model.Order;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderType;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    List<Order> getOrders(String figi);

    Order makeOrder(String orderId, String figi, OrderDirection orderDirection, int lotsNumber, OrderType orderType, BigDecimal price);

    void cancelOrder(String orderId);

}