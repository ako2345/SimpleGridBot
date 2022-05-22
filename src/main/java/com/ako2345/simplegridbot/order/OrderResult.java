package com.ako2345.simplegridbot.order;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public class OrderResult {

    private final boolean isSuccessful;
    private final BigDecimal price;
    private final BigDecimal baseCurrencyAmount;

}