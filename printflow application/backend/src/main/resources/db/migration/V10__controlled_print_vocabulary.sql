CREATE TABLE print_vocabulary (category VARCHAR(40) PRIMARY KEY,choices JSONB NOT NULL,revision INTEGER NOT NULL DEFAULT 1);
INSERT INTO print_vocabulary(category,choices) VALUES
 ('paperType','["UNSURE","ART_PAPER","OFFSET_PAPER","BOARD","CUSTOM"]'),
 ('printSides','["UNSURE","SINGLE","DOUBLE"]'),
 ('colour','["UNSURE","MONO","CMYK","SPOT","CMYK_SPOT"]'),
 ('press','["UNSURE","OFFSET","DIGITAL"]'),
 ('binding','["UNSURE","NONE","STAPLED","PERFECT","SPIRAL"]'),
 ('finishing','["UNSURE","NONE","GLOSS","MATTE","LAMINATION","FOIL","EMBOSS"]');
CREATE TABLE vocabulary_events(id UUID PRIMARY KEY,category VARCHAR(40) NOT NULL,actor_id UUID NOT NULL REFERENCES staff_users(id),revision INTEGER NOT NULL,choices JSONB NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TRIGGER vocabulary_event_immutable BEFORE UPDATE OR DELETE ON vocabulary_events FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
