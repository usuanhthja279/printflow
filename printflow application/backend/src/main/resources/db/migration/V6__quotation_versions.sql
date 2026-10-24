CREATE TABLE quote_versions (
 id UUID PRIMARY KEY,
 enquiry_id UUID NOT NULL REFERENCES enquiries(id),
 version INTEGER NOT NULL CHECK(version>0),
 scope VARCHAR(3000) NOT NULL,
 total_paise BIGINT NOT NULL CHECK(total_paise BETWEEN 1 AND 100000000000),
 terms VARCHAR(2000) NOT NULL,
 requested_date DATE,
 proposed_date DATE NOT NULL,
 valid_until DATE NOT NULL,
 specification_snapshot JSONB NOT NULL,
 issued_by UUID NOT NULL REFERENCES staff_users(id),
 issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 state VARCHAR(24) NOT NULL DEFAULT 'ISSUED' CHECK(state IN ('ISSUED','SUPERSEDED','ACCEPTED','CHANGES_REQUESTED')),
 responded_at TIMESTAMPTZ,
 response_note VARCHAR(1000),
 UNIQUE(enquiry_id,version)
);
CREATE UNIQUE INDEX one_accepted_quote ON quote_versions(enquiry_id) WHERE state='ACCEPTED';
CREATE INDEX quote_history ON quote_versions(enquiry_id,version DESC);
CREATE TABLE quote_events (
 id UUID PRIMARY KEY, quote_id UUID NOT NULL REFERENCES quote_versions(id),
 actor_id UUID NOT NULL REFERENCES staff_users(id),
 action VARCHAR(32) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER quote_event_immutable BEFORE UPDATE OR DELETE ON quote_events FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE FUNCTION protect_quote_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Quotation history cannot be deleted'; END IF;
 IF (to_jsonb(OLD)-ARRAY['state','responded_at','response_note']) IS DISTINCT FROM
    (to_jsonb(NEW)-ARRAY['state','responded_at','response_note']) THEN RAISE EXCEPTION 'Issued quotation content is immutable'; END IF;
 IF OLD.state<>'ISSUED' OR NEW.state NOT IN ('SUPERSEDED','ACCEPTED','CHANGES_REQUESTED') THEN RAISE EXCEPTION 'Invalid quotation transition'; END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER quote_content_immutable BEFORE UPDATE OR DELETE ON quote_versions FOR EACH ROW EXECUTE FUNCTION protect_quote_version();
