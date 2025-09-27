CREATE TABLE webhook_idempotency (
    id UUID PRIMARY KEY,
    source TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    payload_hash TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_webhook_idempotency_source_key UNIQUE (source, idempotency_key)
);