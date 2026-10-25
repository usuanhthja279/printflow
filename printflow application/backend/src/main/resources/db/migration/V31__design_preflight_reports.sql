CREATE TABLE design_preflight_reports (
    id UUID PRIMARY KEY,
    enquiry_id UUID NOT NULL REFERENCES enquiries(id),
    draft_id UUID NOT NULL REFERENCES design_editor_drafts(id),
    draft_revision BIGINT NOT NULL CHECK (draft_revision > 0),
    content_hash CHAR(64) NOT NULL,
    ruleset_version VARCHAR(40) NOT NULL,
    report JSONB NOT NULL CHECK (jsonb_typeof(report) = 'array'),
    requested_by UUID NOT NULL REFERENCES staff_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged_by UUID REFERENCES staff_users(id),
    acknowledged_at TIMESTAMPTZ,
    acknowledgement_note VARCHAR(2000),
    CHECK ((acknowledged_by IS NULL AND acknowledged_at IS NULL AND acknowledgement_note IS NULL)
        OR (acknowledged_by IS NOT NULL AND acknowledged_at IS NOT NULL AND acknowledgement_note IS NOT NULL))
);
CREATE INDEX idx_design_preflight_project_history
    ON design_preflight_reports(enquiry_id, created_at DESC);
