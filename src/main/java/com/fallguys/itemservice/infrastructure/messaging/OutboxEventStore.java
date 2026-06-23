package com.fallguys.itemservice.infrastructure.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventStore {

    void save(OutboxEvent event);

    List<OutboxEvent> findPublishableForUpdate(int limit, int maxAttempts);

    void markPublished(UUID id, Instant publishedAt);

    void markRetry(UUID id, int retryCount, Instant nextRetryAt, String lastError);

    void markFailed(UUID id, int retryCount, String lastError);
}
