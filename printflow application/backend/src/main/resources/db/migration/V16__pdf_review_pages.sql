ALTER TABLE design_artifacts ADD COLUMN page_count INTEGER CHECK(page_count BETWEEN 1 AND 100);
ALTER TABLE proof_decisions ADD COLUMN reviewed_pages JSONB NOT NULL DEFAULT '[]'::jsonb;
