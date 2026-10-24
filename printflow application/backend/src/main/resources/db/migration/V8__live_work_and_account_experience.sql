ALTER TABLE enquiries ADD COLUMN requested_date DATE;
ALTER TABLE enquiries ADD COLUMN closure_kind VARCHAR(16) CHECK(closure_kind IN ('REJECTED','REMOVED','COMPLETED'));
ALTER TABLE enquiries ADD COLUMN closure_message VARCHAR(1000);
ALTER TABLE quote_versions ALTER COLUMN valid_until DROP NOT NULL;
ALTER TABLE quote_versions ADD COLUMN acceptance_source VARCHAR(12) CHECK(acceptance_source IN ('PORTAL','PHONE','EMAIL'));
ALTER TABLE quote_versions ADD COLUMN acceptance_evidence VARCHAR(1000);
ALTER TABLE quote_versions ADD COLUMN acceptance_recorded_by UUID REFERENCES staff_users(id);
CREATE OR REPLACE FUNCTION protect_quote_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Quotation history cannot be deleted'; END IF;
 IF (to_jsonb(OLD)-ARRAY['state','responded_at','response_note','acceptance_source','acceptance_evidence','acceptance_recorded_by']) IS DISTINCT FROM (to_jsonb(NEW)-ARRAY['state','responded_at','response_note','acceptance_source','acceptance_evidence','acceptance_recorded_by']) THEN RAISE EXCEPTION 'Issued quotation content is immutable'; END IF;
 IF OLD.state<>'ISSUED' OR NEW.state NOT IN ('SUPERSEDED','ACCEPTED','CHANGES_REQUESTED') THEN RAISE EXCEPTION 'Invalid quotation transition'; END IF;
 IF NEW.state='ACCEPTED' AND (NEW.acceptance_source IS NULL OR NEW.acceptance_recorded_by IS NULL OR (NEW.acceptance_source IN ('PHONE','EMAIL') AND length(trim(coalesce(NEW.acceptance_evidence,'')))<20)) THEN RAISE EXCEPTION 'Acceptance evidence required'; END IF;
 RETURN NEW;
END; $$;
ALTER TABLE staff_users ADD COLUMN avatar_png BYTEA CHECK(octet_length(avatar_png)<=65536);
ALTER TABLE staff_users ADD COLUMN avatar_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE staff_users ADD COLUMN public_name VARCHAR(100);
ALTER TABLE proof_comments ADD COLUMN anchor_x DOUBLE PRECISION CHECK(anchor_x BETWEEN 0 AND 1);
ALTER TABLE proof_comments ADD COLUMN anchor_y DOUBLE PRECISION CHECK(anchor_y BETWEEN 0 AND 1);
ALTER TABLE proof_comments ADD CONSTRAINT paired_anchors CHECK((anchor_x IS NULL)=(anchor_y IS NULL));
CREATE TABLE work_messages (
 id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES enquiries(id),actor_id UUID NOT NULL REFERENCES staff_users(id),
 author_role VARCHAR(16) NOT NULL CHECK(author_role IN ('TEAM','CUSTOMER')),
 channel VARCHAR(12) NOT NULL DEFAULT 'PORTAL' CHECK(channel IN ('PORTAL','EMAIL')),
 body VARCHAR(3000) NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX work_conversation ON work_messages(enquiry_id,created_at);
CREATE TABLE work_events (
 id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES enquiries(id),actor_id UUID NOT NULL REFERENCES staff_users(id),
 kind VARCHAR(40) NOT NULL,message VARCHAR(1000) NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX work_event_history ON work_events(enquiry_id,created_at);
CREATE TRIGGER work_message_immutable BEFORE UPDATE OR DELETE ON work_messages FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TRIGGER work_event_immutable BEFORE UPDATE OR DELETE ON work_events FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
UPDATE enquiries e SET status='IN_PROGRESS',revision=revision+1 WHERE status<>'CLOSED' AND EXISTS(SELECT 1 FROM quote_versions q WHERE q.enquiry_id=e.id AND q.state='ACCEPTED');
