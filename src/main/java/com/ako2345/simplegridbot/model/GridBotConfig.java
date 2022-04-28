package com.ako2345.simplegridbot.model;

import lombok.Data;

@Data
public class GridBotConfig {
    public String figi;
    public float lowerPrice;
    public float upperPrice;
    public int gridsNumber;
    public float investment;
}