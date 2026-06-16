package com.fallguys.itemservice.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "inventory.client")
public record InventoryClientProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        int retryMaxAttempts
) {

    private static final String DEFAULT_BASE_URL = "http://inventory-service:8080";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(3);
    private static final int DEFAULT_RETRY_MAX_ATTEMPTS = 2;

    public InventoryClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }
        if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
            readTimeout = DEFAULT_READ_TIMEOUT;
        }
        if (retryMaxAttempts < 1) {
            retryMaxAttempts = DEFAULT_RETRY_MAX_ATTEMPTS;
        }
    }
}
