package com.ako2345.simplegridbot.order;

import com.ako2345.simplegridbot.service.AccountService;
import lombok.RequiredArgsConstructor;
import ru.tinkoff.piapi.contract.v1.OrderExecutionReportStatus;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.ako2345.simplegridbot.Constants.DEFAULT_SCALE;

@RequiredArgsConstructor
public class TrueOrderManager implements OrderManager {

    private final AccountService accountService;

    @Override
    public OrderResult buy(String figi, int lotsNumber, BigDecimal lotSize) {
        var orderResponse = accountService.buy(figi, lotsNumber);
        if (orderResponse.getExecutionReportStatus() == OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL
                || orderResponse.getExecutionReportStatus() == OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW) {
            var totalOrderAmount = MapperUtils.moneyValueToBigDecimal(orderResponse.getTotalOrderAmount());
            var orderPrice = totalOrderAmount
                    .divide(lotSize, DEFAULT_SCALE, RoundingMode.DOWN)
                    .divide(BigDecimal.valueOf(lotsNumber), DEFAULT_SCALE, RoundingMode.DOWN);
            return new OrderResult(
                    true,
                    orderPrice,
                    MapperUtils.moneyValueToBigDecimal(orderResponse.getTotalOrderAmount())
            );
        } else {
            throw new RuntimeException("Error while executing buy order (execution report status: " + orderResponse.getExecutionReportStatus() + ")");
        }
    }

    @Override
    public OrderResult sell(String figi, int lotsNumber, BigDecimal lotSize) {
        var orderResponse = accountService.sell(figi, lotsNumber);
        if (orderResponse.getExecutionReportStatus() == OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL
                || orderResponse.getExecutionReportStatus() == OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW) {
            var totalOrderAmount = MapperUtils.moneyValueToBigDecimal(orderResponse.getTotalOrderAmount());
            var orderPrice = totalOrderAmount
                    .divide(lotSize, DEFAULT_SCALE, RoundingMode.DOWN)
                    .divide(BigDecimal.valueOf(lotsNumber), DEFAULT_SCALE, RoundingMode.DOWN);
            return new OrderResult(
                    true,
                    orderPrice,
                    MapperUtils.moneyValueToBigDecimal(orderResponse.getTotalOrderAmount())
            );
        } else {
            throw new RuntimeException("Error while executing sell order (execution report status: " + orderResponse.getExecutionReportStatus() + ")");
        }
    }

}