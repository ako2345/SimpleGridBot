package com.ako2345.simplegridbot.assets;

import java.math.BigDecimal;

public class Assets {

    public BigDecimal baseCurrencyAmount;
    public BigDecimal instrumentAmount;

    public Assets(BigDecimal baseCurrencyAmount, BigDecimal instrumentAmount) {
        this.baseCurrencyAmount = baseCurrencyAmount;
        this.instrumentAmount = instrumentAmount;
    }

    public BigDecimal getBalance(BigDecimal currentPrice) {
        return baseCurrencyAmount.add(instrumentAmount.multiply(currentPrice));
    }

    @Override
    public String toString() {
        return "Base currency amount: " + baseCurrencyAmount + ". Instrument amount: " + instrumentAmount + ".";
    }
}