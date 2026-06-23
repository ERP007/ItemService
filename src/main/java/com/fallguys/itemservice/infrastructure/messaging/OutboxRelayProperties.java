package com.fallguys.itemservice.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "messaging.outbox.relay")
public record OutboxRelayProperties(
        boolean enabled,
        int batchSize,
        long fixedDelayMs,
        int maxAttempts,
        long retryDelaySeconds,
        long confirmTimeoutMs
) {

    public OutboxRelayProperties {
        if (batchSize < 1) {
            batchSize = 50;
        }
        if (fixedDelayMs < 1) {
            fixedDelayMs = 2_000;
        }
        if (maxAttempts < 1) {
            maxAttempts = 10;
        }
        if (retryDelaySeconds < 1) {
            retryDelaySeconds = 30;
        }
        if (confirmTimeoutMs < 1) {
            confirmTimeoutMs = 5_000;
        }
    }

    public Duration retryDelay() {
        return Duration.ofSeconds(retryDelaySeconds);
    }
}
