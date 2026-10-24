CREATE TABLE reference_feedback (
 id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES enquiries(id),artifact_id UUID NOT NULL REFERENCES design_artifacts(id),actor_id UUID NOT NULL REFERENCES staff_users(id),artifact_sha256 CHAR(64) NOT NULL,note VARCHAR(2000) NOT NULL,pins JSONB NOT NULL,review_pdf BYTEA NOT NULL CHECK(octet_length(review_pdf)<=15728640),review_sha256 CHAR(64) NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX reference_feedback_enquiry ON reference_feedback(enquiry_id,created_at DESC);
CREATE TRIGGER reference_feedback_immutable BEFORE UPDATE OR DELETE ON reference_feedback FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
ALTER TABLE mail_outbox ADD COLUMN feedback_id UUID REFERENCES reference_feedback(id);
