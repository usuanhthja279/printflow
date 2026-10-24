ALTER TABLE enquiries ADD COLUMN designer_id UUID REFERENCES staff_users(id);
CREATE INDEX enquiry_designer ON enquiries(designer_id);
CREATE TABLE design_artifacts (
 id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES enquiries(id),uploaded_by UUID NOT NULL REFERENCES staff_users(id),
 display_name VARCHAR(180) NOT NULL,media_type VARCHAR(60) NOT NULL,byte_size BIGINT NOT NULL CHECK(byte_size BETWEEN 1 AND 20971520),
 sha256 CHAR(64) NOT NULL,kind VARCHAR(16) NOT NULL CHECK(kind IN ('REFERENCE','DESIGN')),
 state VARCHAR(16) NOT NULL CHECK(state IN ('QUARANTINED','CLEAN','REJECTED')),scan_engine VARCHAR(40) NOT NULL,
 scanned_at TIMESTAMPTZ,created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX artifact_enquiry ON design_artifacts(enquiry_id,created_at);
CREATE TABLE proof_revisions (
 id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES enquiries(id),revision INTEGER NOT NULL CHECK(revision>0),
 artifact_id UUID NOT NULL REFERENCES design_artifacts(id),quote_id UUID NOT NULL REFERENCES quote_versions(id),
 summary VARCHAR(2000) NOT NULL,created_by UUID NOT NULL REFERENCES staff_users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),UNIQUE(enquiry_id,revision)
);
CREATE TABLE proof_decisions (
 id UUID PRIMARY KEY,proof_id UUID NOT NULL UNIQUE REFERENCES proof_revisions(id),actor_id UUID NOT NULL REFERENCES staff_users(id),
 action VARCHAR(24) NOT NULL CHECK(action IN ('APPROVE','REQUEST_CHANGES')),note VARCHAR(2000) NOT NULL,
 artifact_sha256 CHAR(64) NOT NULL,quote_id UUID NOT NULL REFERENCES quote_versions(id),created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE proof_comments (
 id UUID PRIMARY KEY,proof_id UUID NOT NULL REFERENCES proof_revisions(id),actor_id UUID NOT NULL REFERENCES staff_users(id),
 author_role VARCHAR(16) NOT NULL CHECK(author_role IN ('CUSTOMER','TEAM')),body VARCHAR(2000) NOT NULL,
 page_number INTEGER CHECK(page_number BETWEEN 1 AND 10000),created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX proof_comments_history ON proof_comments(proof_id,created_at);
CREATE TABLE design_events (
 id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES enquiries(id),actor_id UUID NOT NULL REFERENCES staff_users(id),
 action VARCHAR(40) NOT NULL,object_id UUID NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER proof_revision_immutable BEFORE UPDATE OR DELETE ON proof_revisions FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TRIGGER proof_decision_immutable BEFORE UPDATE OR DELETE ON proof_decisions FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TRIGGER proof_comment_immutable BEFORE UPDATE OR DELETE ON proof_comments FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TRIGGER design_event_immutable BEFORE UPDATE OR DELETE ON design_events FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE FUNCTION protect_design_artifact() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR (to_jsonb(OLD)-ARRAY['state','scan_engine','scanned_at']) IS DISTINCT FROM (to_jsonb(NEW)-ARRAY['state','scan_engine','scanned_at']) THEN RAISE EXCEPTION 'Artifact content is immutable'; END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER design_artifact_immutable BEFORE UPDATE OR DELETE ON design_artifacts FOR EACH ROW EXECUTE FUNCTION protect_design_artifact();
