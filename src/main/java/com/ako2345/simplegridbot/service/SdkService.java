package com.ako2345.simplegridbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.core.InvestApi;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdkService {

    private final ConfigService configService;

    private InvestApi investApi;

    public InvestApi getInvestApi() {
        if (investApi == null) {
            var sandBoxMode = configService.getSandboxMode();
            var appName = configService.getAppName();
            var token = configService.getToken();
            log.info("Sandbox mode is {}", sandBoxMode);
            if (sandBoxMode) {
                investApi = InvestApi.createSandbox(token, appName);
            } else {
                investApi = InvestApi.create(token, appName);
            }
        }
        return investApi;
    }

}