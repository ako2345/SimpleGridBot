package com.ako2345.simplegridbot.controller.config;

import lombok.Data;

@Data
public class BacktestConfig {

    public final GridBotConfig gridBotConfig;
    public final int days;
    public final float fee;

}