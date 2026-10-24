ALTER TABLE quote_versions DROP CONSTRAINT quote_versions_state_check;
ALTER TABLE quote_versions ADD CONSTRAINT quote_versions_state_check CHECK(state IN ('ISSUED','SUPERSEDED','ACCEPTED','CHANGES_REQUESTED','WITHDRAWN','REPLACED'));
ALTER TABLE quote_versions ADD COLUMN line_items JSONB NOT NULL DEFAULT '[]';
ALTER TABLE quote_versions ADD COLUMN amendment_of UUID REFERENCES quote_versions(id);
ALTER TABLE quote_versions ADD COLUMN amendment_reason VARCHAR(1000);
ALTER TABLE quote_versions ADD COLUMN withdrawal_reason VARCHAR(1000);
ALTER TABLE quote_versions ADD COLUMN document_pdf BYTEA;
ALTER TABLE quote_versions ADD COLUMN document_sha256 CHAR(64);
CREATE TABLE quote_drafts (
 enquiry_id UUID PRIMARY KEY REFERENCES enquiries(id),revision INTEGER NOT NULL DEFAULT 1 CHECK(revision>0),
 payload JSONB NOT NULL,updated_by UUID NOT NULL REFERENCES staff_users(id),updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE OR REPLACE FUNCTION protect_quote_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Quotation history cannot be deleted'; END IF;
 IF (to_jsonb(OLD)-ARRAY['state','responded_at','response_note','acceptance_source','acceptance_evidence','acceptance_recorded_by','withdrawal_reason']) IS DISTINCT FROM
    (to_jsonb(NEW)-ARRAY['state','responded_at','response_note','acceptance_source','acceptance_evidence','acceptance_recorded_by','withdrawal_reason']) THEN RAISE EXCEPTION 'Issued quotation content is immutable'; END IF;
 IF OLD.state='ACCEPTED' AND NEW.state='REPLACED' THEN
  IF (to_jsonb(OLD)-'state') IS DISTINCT FROM (to_jsonb(NEW)-'state') THEN RAISE EXCEPTION 'Acceptance evidence is immutable'; END IF;
  RETURN NEW;
 END IF;
 IF OLD.state<>'ISSUED' OR NEW.state NOT IN ('SUPERSEDED','ACCEPTED','CHANGES_REQUESTED','WITHDRAWN') THEN RAISE EXCEPTION 'Invalid quotation transition'; END IF;
 IF NEW.state='WITHDRAWN' AND length(trim(coalesce(NEW.withdrawal_reason,'')))<20 THEN RAISE EXCEPTION 'Withdrawal reason required'; END IF;
 IF NEW.state='ACCEPTED' AND (NEW.acceptance_source IS NULL OR NEW.acceptance_recorded_by IS NULL OR (NEW.acceptance_source IN ('PHONE','EMAIL') AND length(trim(coalesce(NEW.acceptance_evidence,'')))<20)) THEN RAISE EXCEPTION 'Acceptance evidence required'; END IF;
 RETURN NEW;
END; $$;
CREATE TABLE commercial_orders (
 id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES enquiries(id),quote_id UUID NOT NULL UNIQUE REFERENCES quote_versions(id),
 order_number BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,scope VARCHAR(3000) NOT NULL,total_paise BIGINT NOT NULL,
 terms VARCHAR(2000) NOT NULL,agreed_date DATE NOT NULL,specification_snapshot JSONB NOT NULL,
 created_by UUID NOT NULL REFERENCES staff_users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE commercial_order_lines (
 id UUID PRIMARY KEY,order_id UUID NOT NULL REFERENCES commercial_orders(id),position INTEGER NOT NULL,
 description VARCHAR(500) NOT NULL,quantity INTEGER NOT NULL CHECK(quantity>0),unit_paise BIGINT NOT NULL CHECK(unit_paise>=0),
 tax_bps INTEGER NOT NULL CHECK(tax_bps BETWEEN 0 AND 10000),total_paise BIGINT NOT NULL,
 specifications JSONB NOT NULL,UNIQUE(order_id,position)
);
CREATE TRIGGER commercial_order_immutable BEFORE UPDATE OR DELETE ON commercial_orders FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TRIGGER commercial_order_line_immutable BEFORE UPDATE OR DELETE ON commercial_order_lines FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TABLE commercial_order_artwork (
 order_line_id UUID NOT NULL REFERENCES commercial_order_lines(id),artifact_id UUID NOT NULL REFERENCES design_artifacts(id),
 linked_by UUID NOT NULL REFERENCES staff_users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT now(),PRIMARY KEY(order_line_id,artifact_id)
);
CREATE TRIGGER order_artwork_immutable BEFORE UPDATE OR DELETE ON commercial_order_artwork FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TABLE design_tasks (
 id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES enquiries(id),title VARCHAR(200) NOT NULL,
 notes VARCHAR(2000) NOT NULL,due_date DATE NOT NULL,state VARCHAR(16) NOT NULL CHECK(state IN ('TODO','IN_PROGRESS','DONE')),
 revision INTEGER NOT NULL DEFAULT 1,created_by UUID NOT NULL REFERENCES staff_users(id),updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE proof_comment_resolutions (
 id UUID PRIMARY KEY,comment_id UUID NOT NULL UNIQUE REFERENCES proof_comments(id),actor_id UUID NOT NULL REFERENCES staff_users(id),
 note VARCHAR(1000) NOT NULL CHECK(length(trim(note))>=10),created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER proof_resolution_immutable BEFORE UPDATE OR DELETE ON proof_comment_resolutions FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
ALTER TABLE proof_decisions ADD COLUMN decision_source VARCHAR(12) NOT NULL DEFAULT 'PORTAL' CHECK(decision_source IN ('PORTAL','PHONE','EMAIL'));
ALTER TABLE proof_decisions DROP CONSTRAINT proof_decisions_action_check;
ALTER TABLE proof_decisions ADD CONSTRAINT proof_decisions_action_check CHECK(action IN ('APPROVE','REQUEST_CHANGES','REJECT'));
ALTER TABLE proof_decisions ADD COLUMN evidence_artifact_id UUID REFERENCES design_artifacts(id);
ALTER TABLE proof_decisions ADD CONSTRAINT offline_proof_evidence CHECK(decision_source='PORTAL' OR (action='APPROVE' AND evidence_artifact_id IS NOT NULL AND length(trim(note))>=20));
