package com.ako2345.simplegridbot.order;

import com.ako2345.simplegridbot.model.Order;
import com.ako2345.simplegridbot.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderType;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class TrueOrderManager implements OrderManager {

    private final OrderService orderService;

    @Override
    public Order makeBuyMarketOrder(String figi, int lotsNumber, BigDecimal lotSize) {
        return orderService.makeOrder(UUID.randomUUID().toString(), figi, OrderDirection.ORDER_DIRECTION_BUY, lotsNumber, OrderType.ORDER_TYPE_MARKET, null);
    }

    @Override
    public Order makeSellMarketOrder(String figi, int lotsNumber, BigDecimal lotSize) {
        return orderService.makeOrder(UUID.randomUUID().toString(), figi, OrderDirection.ORDER_DIRECTION_SELL, lotsNumber, OrderType.ORDER_TYPE_MARKET, null);
    }

    @Override
    public Order makeBuyLimitOrder(String figi, int lotsNumber, BigDecimal lotSize, BigDecimal price) {
        return orderService.makeOrder(UUID.randomUUID().toString(), figi, OrderDirection.ORDER_DIRECTION_BUY, lotsNumber, OrderType.ORDER_TYPE_LIMIT, price);
    }

    @Override
    public Order makeSellLimitOrder(String figi, int lotsNumber, BigDecimal lotSize, BigDecimal price) {
        return orderService.makeOrder(UUID.randomUUID().toString(), figi, OrderDirection.ORDER_DIRECTION_SELL, lotsNumber, OrderType.ORDER_TYPE_LIMIT, price);
    }

    @Override
    public void cancelOrders(String figi) {
        var orders = orderService.getOrders(figi);
        orders.stream()
                .map(Order::getOrderId)
                .forEach(orderService::cancelOrder);
    }

}