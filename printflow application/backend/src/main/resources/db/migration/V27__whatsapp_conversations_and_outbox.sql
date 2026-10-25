CREATE TABLE whatsapp_conversations (
    id UUID PRIMARY KEY,
    phone_hash CHAR(64) NOT NULL UNIQUE,
    phone_ciphertext TEXT NOT NULL,
    contact_name_ciphertext TEXT,
    bot_state VARCHAR(32) NOT NULL DEFAULT 'MENU',
    state_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    handoff_status VARCHAR(20) NOT NULL DEFAULT 'BOT',
    assigned_staff_id UUID REFERENCES staff_users(id),
    last_inbound_at TIMESTAMPTZ,
    last_outbound_at TIMESTAMPTZ,
    suppressed_at TIMESTAMPTZ,
    revision BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE whatsapp_events (
    id UUID PRIMARY KEY,
    provider_event_id VARCHAR(220) NOT NULL UNIQUE,
    event_hash CHAR(64) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'ACCEPTED',
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);
CREATE INDEX idx_whatsapp_events_received ON whatsapp_events(received_at);

CREATE TABLE whatsapp_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES whatsapp_conversations(id),
    provider_message_id VARCHAR(220) UNIQUE,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    direction VARCHAR(12) NOT NULL CHECK (direction IN ('INBOUND','OUTBOUND')),
    message_type VARCHAR(24) NOT NULL,
    body_ciphertext TEXT,
    delivery_status VARCHAR(24) NOT NULL,
    provider_status_at TIMESTAMPTZ,
    provider_error_code VARCHAR(80),
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_whatsapp_messages_conversation ON whatsapp_messages(conversation_id,created_at,id);
CREATE INDEX idx_whatsapp_messages_outbox ON whatsapp_messages(delivery_status,next_attempt_at,created_at) WHERE direction='OUTBOUND';

CREATE TABLE whatsapp_contact_consents (
    id UUID PRIMARY KEY,
    enquiry_id UUID REFERENCES enquiries(id),
    conversation_id UUID REFERENCES whatsapp_conversations(id),
    purpose VARCHAR(32) NOT NULL,
    channel VARCHAR(16) NOT NULL,
    consent_version VARCHAR(32) NOT NULL,
    granted BOOLEAN NOT NULL DEFAULT TRUE,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    withdrawn_at TIMESTAMPTZ,
    CHECK (enquiry_id IS NOT NULL OR conversation_id IS NOT NULL)
);
CREATE INDEX idx_whatsapp_consent_enquiry ON whatsapp_contact_consents(enquiry_id);
CREATE INDEX idx_whatsapp_consent_conversation ON whatsapp_contact_consents(conversation_id,granted_at DESC);

CREATE TABLE whatsapp_handoff_requests (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES whatsapp_conversations(id),
    assigned_staff_id UUID REFERENCES staff_users(id),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ
);
CREATE INDEX idx_whatsapp_handoff_queue ON whatsapp_handoff_requests(status,created_at);
