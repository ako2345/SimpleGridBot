package com.ako2345.simplegridbot.controller.config;

import lombok.Data;

@Data
public class AnalysisConfig {

    public final String figi;
    public final int days;
    public final float fee;

}