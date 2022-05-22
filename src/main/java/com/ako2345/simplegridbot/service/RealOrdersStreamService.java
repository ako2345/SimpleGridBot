package com.ako2345.simplegridbot.service;

import com.ako2345.simplegridbot.Constants;
import com.ako2345.simplegridbot.bot.GridBot;
import com.ako2345.simplegridbot.cache.InstrumentsCache;
import com.ako2345.simplegridbot.cache.OrdersCache;
import com.ako2345.simplegridbot.model.Direction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderTrade;
import ru.tinkoff.piapi.contract.v1.TradesStreamResponse;
import ru.tinkoff.piapi.core.stream.StreamProcessor;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@RestController
@Slf4j
@RequiredArgsConstructor
public class RealOrdersStreamService {

    protected final ConfigService configService;
    protected final SdkService sdkService;
    private final InstrumentsCache instrumentsCache;
    private final OrdersCache ordersCache;
    private final List<GridBot> listeners = new ArrayList<>();

    @PostConstruct
    public void subscribeTrades() {
        if (configService.getSandboxMode()) return;
        var accountId = configService.getAccountId();
        StreamProcessor<TradesStreamResponse> streamProcessor = getStreamProcessor(accountId);
        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());
        var accounts = Collections.singleton(accountId);
        log.info("Subscribing trade orders...");
        sdkService.getInvestApi().getOrdersStreamService().subscribeTrades(streamProcessor, onErrorCallback, accounts);
    }

    public StreamProcessor<TradesStreamResponse> getStreamProcessor(String accountId) {
        return response -> {
            if (response.hasOrderTrades() && !listeners.isEmpty()) {
                var orderTrades = response.getOrderTrades();
                if (!orderTrades.getAccountId().equals(accountId)) return;
                var orderDirection = orderTrades.getDirection();
                var tradesList = orderTrades.getTradesList();
                var figi = orderTrades.getFigi();
                var lotSize = instrumentsCache.getLotSize(figi);
                var totalBaseCurrencyAmount = BigDecimal.ZERO;
                var totalLotsNumber = 0L;
                for (OrderTrade orderTrade : tradesList) {
                    var price = MapperUtils.quotationToBigDecimal(orderTrade.getPrice());
                    var quantity = orderTrade.getQuantity();
                    totalLotsNumber += quantity;
                    totalBaseCurrencyAmount = totalBaseCurrencyAmount.add(price.multiply(BigDecimal.valueOf(quantity)));
                }
                var averageOrderPrice = totalBaseCurrencyAmount.divide(BigDecimal.valueOf(totalLotsNumber).multiply(lotSize), Constants.DEFAULT_SCALE, RoundingMode.DOWN);
                var direction = orderDirection == OrderDirection.ORDER_DIRECTION_SELL ? Direction.SELL : Direction.BUY;
                var orderId = orderTrades.getOrderId();
                ordersCache.addExecutedLotsNumber(orderId, totalLotsNumber);
                ordersCache.addBaseCurrencyAmount(orderId, totalBaseCurrencyAmount);
                if (Constants.LOG_ORDER_STREAM) {
                    log.info(
                            "{} order executed (price: {}). Lots number: {}, order ID: {}",
                            direction,
                            averageOrderPrice.setScale(4, RoundingMode.HALF_DOWN),
                            totalLotsNumber,
                            orderId
                    );
                }
                for (GridBot gridBot : listeners) {
                    var lotsPerGrid = gridBot.getGridManager().getLotsPerGrid();
                    var executedLotsNumber = ordersCache.getExecutedLotsNumber(orderId);
                    var baseCurrencyAmount = ordersCache.getBaseCurrencyAmount(orderId);
                    if (executedLotsNumber == lotsPerGrid) {
                        gridBot.processOrder(figi, direction, averageOrderPrice, baseCurrencyAmount, executedLotsNumber);
                    }
                }
            }
        };
    }

    public void addListener(GridBot gridBot) {
        if (!listeners.contains(gridBot)) {
            listeners.add(gridBot);
        }
    }

    public void removeListener(GridBot gridBot) {
        listeners.remove(gridBot);
    }

}