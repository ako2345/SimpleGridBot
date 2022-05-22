package com.ako2345.simplegridbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ConfigService {

    @Value("${app.config.sandbox-mode}")
    private boolean sandBoxMode;

    @Value("${app.config.app-name}")
    private String appName;

    @Value("${app.config.real-account}")
    private String realAccountId;

    @Value("${app.config.sandbox-account}")
    private String sandboxAccountId;

    @Value("${app.config.real-token}")
    private String realToken;

    @Value("${app.config.sandbox-token}")
    private String sandBoxToken;

    public boolean getSandboxMode() {
        return sandBoxMode;
    }

    public String getAppName() {
        return appName;
    }

    public String getAccountId() {
        var accountId = sandBoxMode ? sandboxAccountId : realAccountId;
        if (!StringUtils.hasLength(accountId)) {
            throw new RuntimeException("Account ID is not set. Check settings in src/main/resources/application.yaml.");
        }
        return accountId;
    }

    public String getToken() {
        var token = sandBoxMode ? sandBoxToken : realToken;
        if (!StringUtils.hasLength(token)) {
            throw new RuntimeException("Token is not set. Check settings in src/main/resources/application.yaml.");
        }
        return token;
    }

}