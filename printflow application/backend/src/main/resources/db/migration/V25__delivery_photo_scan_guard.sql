-- Photos have no PDF page_count. Keep their immutable identity independent of artwork metadata guards.
CREATE FUNCTION protect_delivery_photo() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR (to_jsonb(OLD)-ARRAY['state','scan_engine','scanned_at']) IS DISTINCT FROM (to_jsonb(NEW)-ARRAY['state','scan_engine','scanned_at']) THEN RAISE EXCEPTION 'Delivery photo content is immutable'; END IF;
 RETURN NEW;
END; $$;
DROP TRIGGER delivery_photo_immutable ON delivery_photos;
CREATE TRIGGER delivery_photo_immutable BEFORE UPDATE OR DELETE ON delivery_photos FOR EACH ROW EXECUTE FUNCTION protect_delivery_photo();
