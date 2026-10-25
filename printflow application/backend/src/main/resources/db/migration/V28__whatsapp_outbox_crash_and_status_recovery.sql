ALTER TABLE whatsapp_messages
    ADD COLUMN sending_started_at TIMESTAMPTZ;

CREATE TABLE whatsapp_provider_status_events (
    id UUID PRIMARY KEY,
    provider_message_id VARCHAR(220) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('SENT','DELIVERED','READ','FAILED')),
    provider_status_at TIMESTAMPTZ NOT NULL,
    provider_error_code VARCHAR(80),
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    applied_at TIMESTAMPTZ,
    UNIQUE (provider_message_id,status,provider_status_at)
);

CREATE INDEX idx_whatsapp_provider_status_pending
    ON whatsapp_provider_status_events(provider_message_id,provider_status_at)
    WHERE applied_at IS NULL;
