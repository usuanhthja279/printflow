CREATE TABLE external_identities (
 provider VARCHAR(12) NOT NULL,
 issuer VARCHAR(300) NOT NULL,
 subject VARCHAR(255) NOT NULL,
 user_id UUID NOT NULL REFERENCES staff_users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 PRIMARY KEY(provider,issuer,subject),
 UNIQUE(user_id,provider)
);

ALTER TABLE customer_profiles ADD COLUMN verification_binding CHAR(64);
