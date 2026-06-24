create table outbox_event (
    id uuid primary key,
    event_type varchar(120) not null,
    event_version int not null,
    aggregate_type varchar(60) not null,
    aggregate_id varchar(120) not null,
    exchange_name varchar(120) not null,
    routing_key varchar(180) not null,
    payload jsonb not null,
    status varchar(30) not null,
    retry_count int not null default 0,
    next_retry_at timestamp with time zone,
    last_error text,
    created_at timestamp with time zone not null,
    published_at timestamp with time zone
);

create index idx_outbox_event_publishable
    on outbox_event (status, next_retry_at, created_at);

create index idx_outbox_event_aggregate
    on outbox_event (aggregate_type, aggregate_id);
