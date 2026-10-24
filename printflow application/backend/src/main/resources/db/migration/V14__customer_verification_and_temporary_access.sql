ALTER TABLE identity_tokens ADD COLUMN candidate_password_hash VARCHAR(100);
ALTER TABLE customer_profiles ADD COLUMN temporary_password_expires_at TIMESTAMPTZ;
UPDATE identity_tokens SET used_at=now() WHERE purpose='VERIFY' AND used_at IS NULL AND user_id IN (SELECT user_id FROM customer_profiles WHERE setup_pending);
