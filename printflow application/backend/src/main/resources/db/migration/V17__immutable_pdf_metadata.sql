-- Page count is derived once from the same clean immutable bytes. Content and existing metadata remain protected.
CREATE OR REPLACE FUNCTION protect_design_artifact() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR (to_jsonb(OLD)-ARRAY['state','scan_engine','scanned_at','page_count']) IS DISTINCT FROM (to_jsonb(NEW)-ARRAY['state','scan_engine','scanned_at','page_count']) THEN RAISE EXCEPTION 'Artifact content is immutable'; END IF;
 IF OLD.page_count IS DISTINCT FROM NEW.page_count AND (OLD.page_count IS NOT NULL OR NEW.page_count IS NULL OR OLD.media_type<>'application/pdf' OR OLD.state<>'CLEAN' OR NEW.state<>'CLEAN') THEN RAISE EXCEPTION 'PDF page metadata is immutable after inspection'; END IF;
 RETURN NEW;
END; $$;
