package com.fallguys.itemservice.infrastructure.messaging;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcOutboxEventStore implements OutboxEventStore {

    private static final String INSERT_SQL = """
            insert into outbox_event (
                id, event_type, event_version, aggregate_type, aggregate_id,
                exchange_name, routing_key, payload, status, retry_count,
                next_retry_at, last_error, created_at, published_at
            ) values (
                :id, :eventType, :eventVersion, :aggregateType, :aggregateId,
                :exchangeName, :routingKey, cast(:payload as jsonb), :status, :retryCount,
                :nextRetryAt, :lastError, :createdAt, :publishedAt
            )
            """;

    private static final String FIND_PUBLISHABLE_SQL = """
            select
                id,
                event_type,
                event_version,
                aggregate_type,
                aggregate_id,
                exchange_name,
                routing_key,
                payload::text as payload,
                status,
                retry_count,
                next_retry_at,
                last_error,
                created_at,
                published_at
            from outbox_event
            where status = 'PENDING'
              and retry_count < :maxAttempts
              and (next_retry_at is null or next_retry_at <= current_timestamp)
            order by created_at
            limit :limit
            for update skip locked
            """;

    private static final RowMapper<OutboxEvent> ROW_MAPPER = new OutboxEventRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcOutboxEventStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    public void save(OutboxEvent event) {
        jdbcTemplate.update(INSERT_SQL, parameters(event));
    }

    @Override
    public List<OutboxEvent> findPublishableForUpdate(int limit, int maxAttempts) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("maxAttempts", maxAttempts);
        return jdbcTemplate.query(FIND_PUBLISHABLE_SQL, parameters, ROW_MAPPER);
    }

    @Override
    public void markPublished(UUID id, Instant publishedAt) {
        jdbcTemplate.update("""
                update outbox_event
                set status = 'PUBLISHED',
                    published_at = :publishedAt,
                    last_error = null
                where id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("publishedAt", Timestamp.from(publishedAt)));
    }

    @Override
    public void markRetry(UUID id, int retryCount, Instant nextRetryAt, String lastError) {
        jdbcTemplate.update("""
                update outbox_event
                set retry_count = :retryCount,
                    next_retry_at = :nextRetryAt,
                    last_error = :lastError
                where id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("retryCount", retryCount)
                .addValue("nextRetryAt", Timestamp.from(nextRetryAt))
                .addValue("lastError", lastError));
    }

    @Override
    public void markFailed(UUID id, int retryCount, String lastError) {
        jdbcTemplate.update("""
                update outbox_event
                set status = 'FAILED',
                    retry_count = :retryCount,
                    next_retry_at = null,
                    last_error = :lastError
                where id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("retryCount", retryCount)
                .addValue("lastError", lastError));
    }

    private static MapSqlParameterSource parameters(OutboxEvent event) {
        return new MapSqlParameterSource()
                .addValue("id", event.id())
                .addValue("eventType", event.eventType())
                .addValue("eventVersion", event.eventVersion())
                .addValue("aggregateType", event.aggregateType())
                .addValue("aggregateId", event.aggregateId())
                .addValue("exchangeName", event.exchangeName())
                .addValue("routingKey", event.routingKey())
                .addValue("payload", event.payload())
                .addValue("status", event.status().name())
                .addValue("retryCount", event.retryCount())
                .addValue("nextRetryAt", timestampOrNull(event.nextRetryAt()))
                .addValue("lastError", event.lastError())
                .addValue("createdAt", Timestamp.from(event.createdAt()))
                .addValue("publishedAt", timestampOrNull(event.publishedAt()));
    }

    private static Timestamp timestampOrNull(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static class OutboxEventRowMapper implements RowMapper<OutboxEvent> {

        @Override
        public OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new OutboxEvent(
                    rs.getObject("id", UUID.class),
                    rs.getString("event_type"),
                    rs.getInt("event_version"),
                    rs.getString("aggregate_type"),
                    rs.getString("aggregate_id"),
                    rs.getString("exchange_name"),
                    rs.getString("routing_key"),
                    rs.getString("payload"),
                    OutboxEventStatus.valueOf(rs.getString("status")),
                    rs.getInt("retry_count"),
                    instantOrNull(rs.getTimestamp("next_retry_at")),
                    rs.getString("last_error"),
                    rs.getTimestamp("created_at").toInstant(),
                    instantOrNull(rs.getTimestamp("published_at"))
            );
        }

        private static Instant instantOrNull(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
