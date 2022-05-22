package com.ako2345.simplegridbot.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TextUtils {

    private static final int DEFAULT_SCALE = 2;

    public static String formatProfitPercentage(BigDecimal value) {
        String prefix = value.signum() > 0 ? "+" : "";
        return prefix + value.multiply(BigDecimal.valueOf(100)).setScale(DEFAULT_SCALE, RoundingMode.DOWN) + "%";
    }

    public static String formatProfit(BigDecimal value) {
        String prefix = value.signum() > 0 ? "+" : "";
        return prefix + value.setScale(DEFAULT_SCALE, RoundingMode.DOWN);
    }

}
