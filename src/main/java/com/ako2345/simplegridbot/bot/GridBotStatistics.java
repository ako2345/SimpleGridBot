package com.ako2345.simplegridbot.bot;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GridBotStatistics {

    private final BigDecimal totalProfitPercentage;
    private final BigDecimal totalProfit;
    private final BigDecimal gridProfitPercentage;
    private final BigDecimal gridProfit;
    private final BigDecimal unrealizedProfitPercentage;
    private final BigDecimal unrealizedProfit;
    private final int transactionsNumber;

}