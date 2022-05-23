package com.ako2345.simplegridbot.service;

import com.ako2345.simplegridbot.Constants;
import com.ako2345.simplegridbot.bot.GridBot;
import com.ako2345.simplegridbot.bot.grid.Grid;
import com.ako2345.simplegridbot.cache.InstrumentsCache;
import com.ako2345.simplegridbot.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.core.stream.StreamProcessor;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * В режиме "песочницы" нельзя использовать OrdersStreamService, поэтому придётся эмулировать информирование об
 * исполнении ордеров.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class SandboxOrdersStreamService {

    protected final ConfigService configService;
    protected final SdkService sdkService;
    protected final InfoService infoService;
    protected final SandboxOrderService sandboxOrderService;
    protected final InstrumentsCache instrumentsCache;
    private final List<OrdersStreamServiceListener> listeners;
    private Timer timer;
    private BigDecimal previousPrice;
    private List<Order> previousOrders;

    private void subscribePrices(String figi, Grid grid) {
        if (!configService.getSandboxMode()) return;
        previousOrders = sandboxOrderService.getOrders(figi);
        previousPrice = infoService.getLastPrice(figi);
        StreamProcessor<MarketDataResponse> processor = response -> {
            var responseFigi = response.getLastPrice().getFigi();
            if (response.hasLastPrice() && figi.equals(responseFigi)) {
                var currentPriceQuotation = response.getLastPrice().getPrice();
                var currentPrice = MapperUtils.quotationToBigDecimal(currentPriceQuotation);
                if (Constants.LOG_NEW_PRICE) {
                    log.info("Price for FIGI {}: {}", figi, currentPrice.setScale(4, RoundingMode.DOWN));
                }
                var currentPriceRangeIndex = grid.getPriceRangeIndex(currentPrice);
                var previousPriceRangeIndex = grid.getPriceRangeIndex(previousPrice);
                if (currentPriceRangeIndex != previousPriceRangeIndex) {
                    syncOrders(figi);
                }
                previousPrice = currentPrice;
            }
        };
        infoService.subscribePrice(figi, processor);
    }

    private void unsubscribePrices(String figi) {
        infoService.unsubscribePrice(figi);
    }

    private void startPeriodicOrdersSync(String figi) {
        var task = new TimerTask() {

            @Override
            public void run() {
                syncOrders(figi);
            }

        };
        timer = new Timer();
        timer.scheduleAtFixedRate(task, 0, 4000L);
    }

    private void stopPeriodicOrdersSync() {
        timer.cancel();
    }

    private void syncOrders(String figi) {
        var currentOrders = sandboxOrderService.getOrders(figi);
        var executedOrders = new ArrayList<Order>();
        for (Order order : previousOrders) {
            if (!currentOrders.contains(order)) {
                log.info(
                        "Executed order detected (order ID: {}, price: {})",
                        order.getOrderId(),
                        order.getPrice().setScale(4, RoundingMode.HALF_DOWN)
                );
                executedOrders.add(order);
            }
        }
        previousOrders = currentOrders;
        for (Order executedOrder : executedOrders) {
            var direction = executedOrder.getDirection();
            var price = executedOrder.getPrice();
            var baseCurrencyAmount = executedOrder.getBaseCurrencyAmount();
            var lotsNumber = executedOrder.getLotsNumber();
            for (OrdersStreamServiceListener listener : listeners) {
                listener.processOrder(
                        figi,
                        direction,
                        price,
                        baseCurrencyAmount,
                        lotsNumber
                );
            }
        }
    }

    public void addListener(GridBot gridBot) {
        if (!listeners.contains(gridBot)) {
            listeners.add(gridBot);
            subscribePrices(gridBot.getFigi(), gridBot.getGridManager().getGrid());
            startPeriodicOrdersSync(gridBot.getFigi());
        }
    }

    public void removeListener(GridBot gridBot) {
        listeners.remove(gridBot);
        unsubscribePrices(gridBot.getFigi());
        stopPeriodicOrdersSync();
    }

}