ALTER TABLE staff_users DROP CONSTRAINT staff_users_role_check;
ALTER TABLE staff_users ADD CONSTRAINT staff_users_role_check CHECK
  (role IN ('OWNER','SALES','ESTIMATOR','DESIGNER','SUPERVISOR','OPERATOR','INVENTORY','ACCOUNTS','DELIVERY'));
ALTER TABLE staff_users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE staff_users ADD COLUMN totp_secret TEXT;
ALTER TABLE staff_users ADD COLUMN totp_counter BIGINT NOT NULL DEFAULT -1;
ALTER TABLE staff_users ADD COLUMN mfa_failures INTEGER NOT NULL DEFAULT 0;
ALTER TABLE staff_users ADD COLUMN mfa_window TIMESTAMPTZ NOT NULL DEFAULT NOW();
CREATE TABLE owner_recovery_codes (
  staff_id UUID NOT NULL REFERENCES staff_users(id),
  code_hash CHAR(64) NOT NULL,
  PRIMARY KEY(staff_id,code_hash)
);
CREATE TABLE security_activities (
  id UUID PRIMARY KEY,
  actor_id UUID NOT NULL REFERENCES staff_users(id),
  target_id UUID NOT NULL REFERENCES staff_users(id),
  action VARCHAR(40) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE FUNCTION reject_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Audit history is append-only'; END;
$$;
CREATE TRIGGER security_activity_immutable BEFORE UPDATE OR DELETE ON security_activities
  FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TRIGGER lead_activity_immutable BEFORE UPDATE OR DELETE ON lead_activities
  FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

ALTER TABLE staff_users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
