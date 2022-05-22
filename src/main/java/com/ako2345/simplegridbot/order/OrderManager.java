package com.ako2345.simplegridbot.order;

import java.math.BigDecimal;

public interface OrderManager {

    OrderResult buy(String figi, int lotsNumber, BigDecimal lotSize);

    OrderResult sell(String figi, int lotsNumber, BigDecimal lotSize);

}