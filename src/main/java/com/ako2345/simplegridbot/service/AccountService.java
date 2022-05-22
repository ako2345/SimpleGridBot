package com.ako2345.simplegridbot.service;

import ru.tinkoff.piapi.contract.v1.PostOrderResponse;

public interface AccountService {

    PostOrderResponse buy(String figi, int lotsNumber);

    PostOrderResponse sell(String figi, int lotsNumber);

}