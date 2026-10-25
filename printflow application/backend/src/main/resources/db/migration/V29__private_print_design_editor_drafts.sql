CREATE TABLE design_editor_drafts (
    id UUID PRIMARY KEY,
    enquiry_id UUID NOT NULL UNIQUE REFERENCES enquiries(id),
    revision BIGINT NOT NULL CHECK (revision > 0),
    schema_version SMALLINT NOT NULL CHECK (schema_version = 1),
    document JSONB NOT NULL CHECK (jsonb_typeof(document) = 'object'),
    updated_by UUID NOT NULL REFERENCES staff_users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_design_editor_drafts_updated ON design_editor_drafts(updated_at);
