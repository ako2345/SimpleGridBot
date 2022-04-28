package com.ako2345.simplegridbot.order;

import java.math.BigDecimal;

public interface OrderManager {

    OrderResult buy(int lotsPerGrid, BigDecimal currentPrice);

    OrderResult sell(int lotsPerGrid, BigDecimal currentPrice);

}