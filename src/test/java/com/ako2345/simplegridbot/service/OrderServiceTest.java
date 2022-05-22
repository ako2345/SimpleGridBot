package com.ako2345.simplegridbot.service;

import com.ako2345.simplegridbot.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class OrderServiceTest {

    public static final String FIGI = "BBG004730N88"; // Sber
    public static final int LOTS_NUMBER = 2;
    public static final BigDecimal PRICE = new BigDecimal(45);

    @Autowired
    private SandboxOrderService sandboxOrderService;

    @Autowired
    private SdkService sdkService;

    @Value("${app.config.sandbox-token}")
    private String token;

    @Value("${app.config.sandbox-account}")
    private String accountId;

    @Test
    public void testMakeLimitOrder() {
        var orderId = UUID.randomUUID().toString();
        sandboxOrderService.makeOrder(orderId, FIGI, OrderDirection.ORDER_DIRECTION_BUY, LOTS_NUMBER, OrderType.ORDER_TYPE_LIMIT, PRICE);
        var orders = sandboxOrderService.getOrders(FIGI);
        assertFalse(orders.isEmpty());
        Order recentOrder = null;
        for (Order order : orders) {
            if (order.getFigi().equals(FIGI) &&
                    order.getPrice().compareTo(PRICE) == 0 &&
                    order.getLotsNumber() == LOTS_NUMBER) {
                recentOrder = order;
                break;
            }
        }
        assertNotNull(recentOrder);
        sdkService.getInvestApi().getSandboxService().cancelOrder(accountId, orderId);
    }

}