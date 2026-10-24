ALTER TABLE customer_profiles ADD COLUMN setup_pending BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE customer_profiles ALTER COLUMN consent_at DROP NOT NULL;
CREATE TABLE customer_setup_invites(digest CHAR(64) PRIMARY KEY,user_id UUID NOT NULL REFERENCES staff_users(id),enquiry_id UUID NOT NULL REFERENCES enquiries(id),expires_at TIMESTAMPTZ NOT NULL,used_at TIMESTAMPTZ,created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX customer_setup_user ON customer_setup_invites(user_id,expires_at);
ALTER TABLE mail_outbox ADD COLUMN failure_code VARCHAR(32);
ALTER TABLE work_events ALTER COLUMN actor_id DROP NOT NULL;
CREATE TABLE fulfilment_work(enquiry_id UUID PRIMARY KEY REFERENCES enquiries(id),state VARCHAR(24) NOT NULL CHECK(state IN ('PRODUCTION','READY','IN_TRANSIT','DELIVERED','COMPLETED')),revision BIGINT NOT NULL DEFAULT 1,proof_id UUID NOT NULL REFERENCES proof_revisions(id),quote_id UUID NOT NULL REFERENCES quote_versions(id),released_by UUID NOT NULL REFERENCES staff_users(id),delivered_at TIMESTAMPTZ,issue_open BOOLEAN NOT NULL DEFAULT false,updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
