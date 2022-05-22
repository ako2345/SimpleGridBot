package com.ako2345.simplegridbot.util;

import com.ako2345.simplegridbot.model.Direction;
import com.ako2345.simplegridbot.model.Order;
import com.ako2345.simplegridbot.model.OrderStatus;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderExecutionReportStatus;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;
import ru.tinkoff.piapi.core.utils.MapperUtils;

public class OrderUtils {

    public static Order orderStateToOrder(OrderState orderState) {
        var orderId = orderState.getOrderId();
        var figi = orderState.getFigi();
        var price = MapperUtils.moneyValueToBigDecimal(orderState.getAveragePositionPrice());
        var direction = orderState.getDirection() == OrderDirection.ORDER_DIRECTION_SELL ? Direction.SELL : Direction.BUY;
        var lotsNumber = orderState.getLotsRequested();
        var baseCurrencyAmount = MapperUtils.moneyValueToBigDecimal(orderState.getTotalOrderAmount());
        var orderStatus = convertOrderStatus(orderState.getExecutionReportStatus());
        return new Order(orderId, figi, price, direction, lotsNumber, baseCurrencyAmount, orderStatus);
    }

    public static Order postOrderResponseToOrder(PostOrderResponse postOrderResponse) {
        var orderId = postOrderResponse.getOrderId();
        var figi = postOrderResponse.getFigi();
        var price = MapperUtils.moneyValueToBigDecimal(postOrderResponse.getExecutedOrderPrice());
        var direction = postOrderResponse.getDirection() == OrderDirection.ORDER_DIRECTION_SELL ? Direction.SELL : Direction.BUY;
        var lotsNumber = postOrderResponse.getLotsExecuted();
        var baseCurrencyAmount = MapperUtils.moneyValueToBigDecimal(postOrderResponse.getTotalOrderAmount());
        var orderStatus = convertOrderStatus(postOrderResponse.getExecutionReportStatus());
        return new Order(orderId, figi, price, direction, lotsNumber, baseCurrencyAmount, orderStatus);
    }

    public static OrderStatus convertOrderStatus(OrderExecutionReportStatus orderExecutionReportStatus) {
        switch (orderExecutionReportStatus) {
            case EXECUTION_REPORT_STATUS_FILL:
                return OrderStatus.FILL;
            case EXECUTION_REPORT_STATUS_NEW:
                return OrderStatus.NEW;
            case EXECUTION_REPORT_STATUS_CANCELLED:
                return OrderStatus.CANCELLED;
            case EXECUTION_REPORT_STATUS_REJECTED:
                return OrderStatus.REJECTED;
            case EXECUTION_REPORT_STATUS_PARTIALLYFILL:
                return OrderStatus.PARTIALLY_FILL;
            default:
                throw new RuntimeException("Unknown OrderExecutionReportStatus: " + orderExecutionReportStatus);
        }
    }

}