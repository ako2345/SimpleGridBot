package com.ako2345.simplegridbot.order;

import com.ako2345.simplegridbot.bot.grid.Grid;
import com.ako2345.simplegridbot.model.Direction;
import com.ako2345.simplegridbot.model.Order;
import com.ako2345.simplegridbot.model.OrderStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Обработчик ордеров для бэктеста.
 */
public class FakeOrderManager implements OrderManager {

    private final List<Order> fakeOrders = new ArrayList<>();
    private BigDecimal simulatedPrice;

    @Override
    public Order makeBuyMarketOrder(String figi, int lotsNumber, BigDecimal lotSize) {
        if (simulatedPrice == null) throw new RuntimeException("Simulated price not set");
        var baseCurrencyAmount = simulatedPrice.multiply(lotSize).multiply(BigDecimal.valueOf(lotsNumber));
        return new Order("", figi, simulatedPrice, Direction.BUY, lotsNumber, baseCurrencyAmount, OrderStatus.FILL);
    }

    @Override
    public Order makeSellMarketOrder(String figi, int lotsNumber, BigDecimal lotSize) {
        if (simulatedPrice == null) throw new RuntimeException("Simulated price not set");
        var baseCurrencyAmount = simulatedPrice.multiply(lotSize).multiply(BigDecimal.valueOf(lotsNumber));
        return new Order("", figi, simulatedPrice, Direction.SELL, lotsNumber, baseCurrencyAmount, OrderStatus.FILL);
    }

    @Override
    public Order makeBuyLimitOrder(String figi, int lotsNumber, BigDecimal lotSize, BigDecimal price) {
        var baseCurrencyAmount = price.multiply(lotSize).multiply(BigDecimal.valueOf(lotsNumber));
        var order = new Order("", figi, price, Direction.BUY, lotsNumber, baseCurrencyAmount, OrderStatus.FILL);
        fakeOrders.add(order);
        return order;
    }

    @Override
    public Order makeSellLimitOrder(String figi, int lotsNumber, BigDecimal lotSize, BigDecimal price) {
        var baseCurrencyAmount = price.multiply(lotSize).multiply(BigDecimal.valueOf(lotsNumber));
        var order = new Order("", figi, price, Direction.SELL, lotsNumber, baseCurrencyAmount, OrderStatus.FILL);
        fakeOrders.add(order);
        return order;
    }

    /**
     * Возвращает ордера, которые бы исполнились после изменения цены.
     **/
    public List<Order> getOrdersToExecute(BigDecimal currentPrice, BigDecimal previousPrice, Grid grid) {
        var ordersToExecute = new ArrayList<Order>();
        var currentPriceRangeIndex = grid.getPriceRangeIndex(currentPrice);
        var previousPriceRangeIndex = grid.getPriceRangeIndex(previousPrice);
        if (currentPriceRangeIndex != previousPriceRangeIndex) {
            for (Order fakeOrder : fakeOrders) {
                var price = fakeOrder.getPrice();
                if (price.compareTo(previousPrice) < 0 && price.compareTo(currentPrice) >= 0) {
                    ordersToExecute.add(fakeOrder);
                } else if (price.compareTo(previousPrice) > 0 && price.compareTo(currentPrice) <= 0) {
                    ordersToExecute.add(fakeOrder);
                }
            }
            for (Order orderToExecute : ordersToExecute) {
                fakeOrders.remove(orderToExecute);
            }
        }
        return ordersToExecute;
    }

    @Override
    public void cancelOrders(String figi) {
    }

    public void setSimulatedPrice(BigDecimal simulatedPrice) {
        this.simulatedPrice = simulatedPrice;
    }

}