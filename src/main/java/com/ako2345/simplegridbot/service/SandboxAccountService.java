package com.ako2345.simplegridbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.PortfolioResponse;
import ru.tinkoff.piapi.core.SandboxService;
import ru.tinkoff.piapi.core.utils.MapperUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class SandboxAccountService {

    private final SdkService sdkService;

    @Value("${app.config.sandbox-account}")
    private String accountId;

    public void getFundsInfo() {
        var portfolio = getPortfolio();
        var currencies = MapperUtils.moneyValueToBigDecimal(portfolio.getTotalAmountCurrencies());
        var etfs = MapperUtils.moneyValueToBigDecimal(portfolio.getTotalAmountEtf());
        var bonds = MapperUtils.moneyValueToBigDecimal(portfolio.getTotalAmountBonds());
        var futures = MapperUtils.moneyValueToBigDecimal(portfolio.getTotalAmountFutures());
        var shares = MapperUtils.moneyValueToBigDecimal(portfolio.getTotalAmountShares());
        var total = currencies.add(etfs).add(bonds).add(futures).add(shares);
        log.info("Current funds: {} rub", total);
    }

    public PortfolioResponse getPortfolio() {
        return sdkService.getInvestApi().getSandboxService().getPortfolioSync(getAccountId());
    }

    public String getAccountId() {
        if (!StringUtils.hasLength(accountId)) {
            log.info("Sandbox account is not set. Creating a new one.");
            var sandboxService = sdkService.getInvestApi().getSandboxService();
            accountId = sandboxService.openAccountSync();
            log.info("New sandbox account created. Account ID: {}", accountId);
            initSandboxBalance(sandboxService, accountId);
        }
        return accountId;
    }

    private void initSandboxBalance(SandboxService sandboxService, String accountId) {
        var amount = 100000;
        var currency = "usd"; // or "rub"
        sandboxService.payIn(accountId, MoneyValue.newBuilder().setCurrency(currency).setUnits(amount).build());
        log.info("Funds added for sandbox account ({}). Amount: {} {}", accountId, amount, currency);
    }

}