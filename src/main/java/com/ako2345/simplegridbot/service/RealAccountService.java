package com.ako2345.simplegridbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.*;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealAccountService implements AccountService {

    private final SdkService sdkService;
    private final ConfigService configService;

    @Override
    public PostOrderResponse buy(String figi, int lotsNumber) {
        var orderId = UUID.randomUUID().toString();
        var orderResponse = sdkService.getInvestApi().getOrdersService().postOrderSync(
                figi,
                lotsNumber,
                Quotation.getDefaultInstance(),
                OrderDirection.ORDER_DIRECTION_BUY,
                configService.getAccountId(),
                OrderType.ORDER_TYPE_MARKET,
                orderId
        );
        if (orderResponse.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW &&
                orderResponse.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL) {
            log.warn("Unexpected order result: {}", orderResponse.getExecutionReportStatus());
        }
        return orderResponse;
    }

    @Override
    public PostOrderResponse sell(String figi, int lotsNumber) {
        var orderId = UUID.randomUUID().toString();
        var orderResponse = sdkService.getInvestApi().getOrdersService().postOrderSync(
                figi,
                lotsNumber,
                Quotation.getDefaultInstance(),
                OrderDirection.ORDER_DIRECTION_SELL,
                configService.getAccountId(),
                OrderType.ORDER_TYPE_MARKET,
                orderId
        );
        if (orderResponse.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW &&
                orderResponse.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL) {
            log.warn("Unexpected order result: {}", orderResponse.getExecutionReportStatus());
        }
        return orderResponse;
    }

}