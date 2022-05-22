package com.ako2345.simplegridbot.bot;

import com.ako2345.simplegridbot.util.TextUtils;
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
    private final int arbitragesNumber;

    @Override
    public String toString() {
        return "total profit: " + TextUtils.formatProfit(totalProfit) +
                " (" + TextUtils.formatProfitPercentage(totalProfitPercentage) + ")" +
                ", grid profit: " + TextUtils.formatProfit(gridProfit) +
                " (" + TextUtils.formatProfitPercentage(gridProfitPercentage) + ")" +
                ", unrealized profit: " + TextUtils.formatProfit(unrealizedProfit) +
                " (" + TextUtils.formatProfitPercentage(unrealizedProfitPercentage) + ")" +
                ", transactions: " + transactionsNumber +
                ", arbitrages: " + arbitragesNumber;
    }

}