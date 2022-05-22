package com.ako2345.simplegridbot.service;

import com.ako2345.simplegridbot.cache.OrdersCache;
import com.ako2345.simplegridbot.model.Order;
import com.ako2345.simplegridbot.util.OrderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderExecutionReportStatus;
import ru.tinkoff.piapi.contract.v1.OrderType;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealOrderService implements OrderService {

    private final SdkService sdkService;
    private final ConfigService configService;
    private final OrdersCache ordersCache;

    @Override
    public List<Order> getOrders(String figi) {
        var orders = sdkService.getInvestApi().getOrdersService().getOrdersSync(configService.getAccountId());
        return orders
                .stream()
                .filter(order -> order.getFigi().equals(figi))
                .map(OrderUtils::orderStateToOrder)
                .collect(Collectors.toList());
    }

    @Override
    public Order makeOrder(String orderId, String figi, OrderDirection orderDirection, int lotsNumber, OrderType orderType, BigDecimal price) {
        var quotationPrice = orderType == OrderType.ORDER_TYPE_LIMIT ?
                MapperUtils.bigDecimalToQuotation(price.setScale(2, RoundingMode.HALF_DOWN)) :
                Quotation.getDefaultInstance();
        var postOrderResponse = sdkService.getInvestApi().getOrdersService().postOrderSync(
                figi,
                lotsNumber,
                quotationPrice,
                orderDirection,
                configService.getAccountId(),
                orderType,
                orderId
        );
        ordersCache.addExecutedLotsNumber(postOrderResponse.getOrderId(), 0L);
        ordersCache.addBaseCurrencyAmount(postOrderResponse.getOrderId(), BigDecimal.ZERO);
        if (postOrderResponse.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW &&
                postOrderResponse.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL) {
            log.warn("Unexpected order result: {}", postOrderResponse.getExecutionReportStatus());
        } else {
            log.info(
                    "New order made. Direction: {}, type: {}, price: {}, lots number: {}, order ID: {}",
                    orderDirection,
                    orderType,
                    price != null ? price.setScale(4, RoundingMode.HALF_DOWN) : "",
                    lotsNumber,
                    postOrderResponse.getOrderId()
            );
        }
        return OrderUtils.postOrderResponseToOrder(postOrderResponse);
    }

    @Override
    public void cancelOrder(String orderId) {
        sdkService.getInvestApi().getOrdersService().cancelOrderSync(configService.getAccountId(), orderId);
    }

}