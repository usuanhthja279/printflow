ALTER TABLE staff_users DROP CONSTRAINT staff_users_role_check;
ALTER TABLE staff_users ADD CONSTRAINT staff_users_role_check CHECK (role IN
 ('OWNER','SALES','ESTIMATOR','DESIGNER','SUPERVISOR','OPERATOR','INVENTORY','ACCOUNTS','DELIVERY','CUSTOMER'));
CREATE TABLE customer_profiles (
 user_id UUID PRIMARY KEY REFERENCES staff_users(id),
 name VARCHAR(140) NOT NULL, phone VARCHAR(32) NOT NULL,
 email_verified BOOLEAN NOT NULL DEFAULT FALSE,
 privacy_version VARCHAR(32) NOT NULL, consent_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE identity_tokens (
 digest CHAR(64) PRIMARY KEY, user_id UUID NOT NULL REFERENCES staff_users(id),
 purpose VARCHAR(12) NOT NULL CHECK(purpose IN ('VERIFY','RESET')),
 expires_at TIMESTAMPTZ NOT NULL, used_at TIMESTAMPTZ
);
CREATE TABLE mail_outbox (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES staff_users(id),
 recipient VARCHAR(254) NOT NULL, kind VARCHAR(12) NOT NULL,
 payload TEXT, attempts INTEGER NOT NULL DEFAULT 0,
 state VARCHAR(12) NOT NULL DEFAULT 'PENDING',
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), sent_at TIMESTAMPTZ
);
CREATE INDEX idx_mail_pending ON mail_outbox(state,created_at);
CREATE TABLE request_limits (key_hash CHAR(64) PRIMARY KEY, window_start TIMESTAMPTZ NOT NULL DEFAULT NOW(), hits INTEGER NOT NULL DEFAULT 1);
CREATE TABLE crm_contacts (
 id UUID PRIMARY KEY, owner_id UUID NOT NULL REFERENCES staff_users(id),
 customer_user_id UUID REFERENCES staff_users(id), name VARCHAR(140) NOT NULL,
 email VARCHAR(254) NOT NULL, phone VARCHAR(32) NOT NULL,
 organization VARCHAR(160) NOT NULL DEFAULT '', address VARCHAR(1000) NOT NULL DEFAULT '',
 permission_to_contact BOOLEAN NOT NULL DEFAULT FALSE, consent_note VARCHAR(500) NOT NULL DEFAULT '',
 revision BIGINT NOT NULL DEFAULT 0, merged_into UUID REFERENCES crm_contacts(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE contact_activities (
 id UUID PRIMARY KEY, actor_id UUID NOT NULL REFERENCES staff_users(id),
 contact_id UUID NOT NULL REFERENCES crm_contacts(id), action VARCHAR(32) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER contact_activity_immutable BEFORE UPDATE OR DELETE ON contact_activities
 FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
ALTER TABLE enquiries ADD COLUMN customer_user_id UUID REFERENCES staff_users(id);
ALTER TABLE enquiries ADD COLUMN contact_id UUID REFERENCES crm_contacts(id);
ALTER TABLE enquiries ADD COLUMN template_revision BIGINT;
ALTER TABLE enquiries ADD COLUMN template_snapshot TEXT;
ALTER TABLE enquiries ADD COLUMN privacy_version VARCHAR(32);
CREATE INDEX idx_customer_enquiries ON enquiries(customer_user_id,created_at DESC,id);
CREATE TABLE privacy_requests (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES staff_users(id),
 kind VARCHAR(12) NOT NULL CHECK(kind IN ('EXPORT','ERASURE')),
 status VARCHAR(12) NOT NULL DEFAULT 'OPEN', created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 resolved_at TIMESTAMPTZ, resolution VARCHAR(1000)
);
CREATE TABLE product_templates (
 code VARCHAR(32) PRIMARY KEY, name VARCHAR(140) NOT NULL, description VARCHAR(1000) NOT NULL,
 revision BIGINT NOT NULL DEFAULT 1, active BOOLEAN NOT NULL DEFAULT TRUE,
 min_quantity INTEGER NOT NULL DEFAULT 1, max_quantity INTEGER NOT NULL DEFAULT 10000000,
 gsm_options VARCHAR(1000) NOT NULL DEFAULT '[]', sizes VARCHAR(2000) NOT NULL DEFAULT '[]',
 finishes VARCHAR(2000) NOT NULL DEFAULT '["UNSURE","NONE","GLOSS","MATTE"]',
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
INSERT INTO product_templates(code,name,description) VALUES
 ('WEDDING_CARDS','Shaadi & Wedding Cards','Wedding invitations and celebration stationery'),
 ('HOTEL_PRINT','Hotel Print Materials','Menus, table cards and hotel stationery'),
 ('BROCHURES','Pamphlets & Leaflets','Promotional pamphlets, leaflets and brochures'),
 ('OFFSET_GLOSS','Offset Gloss Printing','Custom offset printing and gloss finish enquiries');
CREATE TABLE template_activities (
 id UUID PRIMARY KEY, actor_id UUID NOT NULL REFERENCES staff_users(id),
 code VARCHAR(32) NOT NULL REFERENCES product_templates(code), revision BIGINT NOT NULL,
 snapshot TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER template_activity_immutable BEFORE UPDATE OR DELETE ON template_activities
 FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

ALTER TABLE customer_profiles ADD COLUMN address VARCHAR(1000) NOT NULL DEFAULT '';
ALTER TABLE customer_profiles ADD COLUMN organization VARCHAR(160) NOT NULL DEFAULT '';
