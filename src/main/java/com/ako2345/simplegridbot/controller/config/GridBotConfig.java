package com.ako2345.simplegridbot.controller.config;

import lombok.Data;

@Data
public class GridBotConfig {

    public final String figi;
    public final float lowerPrice;
    public final float upperPrice;
    public final int gridsNumber;
    public final float investment;

    public GridBotConfig(String figi, float lowerPrice, float upperPrice, int gridsNumber, float investment) {
        this.figi = figi;
        this.lowerPrice = lowerPrice;
        this.upperPrice = upperPrice;
        this.gridsNumber = gridsNumber;
        this.investment = investment;
    }

}