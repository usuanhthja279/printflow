ALTER TABLE design_editor_drafts
    DROP CONSTRAINT design_editor_drafts_schema_version_check;

ALTER TABLE design_editor_drafts
    ADD CONSTRAINT design_editor_drafts_schema_version_check
    CHECK (schema_version IN (1, 2));

UPDATE design_editor_drafts
SET schema_version = 2
WHERE schema_version = 1
  AND jsonb_typeof(document->'pages') = 'array';
