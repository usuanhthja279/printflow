ALTER TABLE enquiries ADD COLUMN intended_use VARCHAR(240);

-- The raw browser key is never persisted. The token needed to replay a successful
-- response is encrypted with the existing server-side application key.
CREATE TABLE enquiry_request_idempotency (
    key_hash CHAR(64) PRIMARY KEY,
    request_hash CHAR(64) NOT NULL,
    public_id UUID NOT NULL,
    tracking_token_ciphertext TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_enquiry_request_idempotency_created ON enquiry_request_idempotency(created_at);
