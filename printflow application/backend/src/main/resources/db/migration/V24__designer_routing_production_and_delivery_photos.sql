ALTER TABLE staff_users ADD COLUMN designer_available BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE staff_users ADD COLUMN routing_pool VARCHAR(8) NOT NULL DEFAULT 'BUSINESS' CHECK(routing_pool IN ('BUSINESS','TEST'));
ALTER TABLE enquiries ADD COLUMN designer_assignment_mode VARCHAR(8) NOT NULL DEFAULT 'AUTO' CHECK(designer_assignment_mode IN ('AUTO','MANUAL'));
CREATE TABLE designer_allocations(id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES enquiries(id),designer_id UUID NOT NULL REFERENCES staff_users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX designer_allocation_load ON designer_allocations(designer_id,created_at);
CREATE TRIGGER designer_allocation_immutable BEFORE UPDATE OR DELETE ON designer_allocations FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

CREATE TABLE production_jobs(enquiry_id UUID PRIMARY KEY REFERENCES fulfilment_work(enquiry_id),stage VARCHAR(12) NOT NULL DEFAULT 'QUEUED' CHECK(stage IN ('QUEUED','PRINTING','FINISHING','QC','READY')),operator_id UUID REFERENCES staff_users(id),machine VARCHAR(80) NOT NULL DEFAULT '',priority VARCHAR(8) NOT NULL DEFAULT 'NORMAL' CHECK(priority IN ('NORMAL','URGENT')),due_date DATE,hold_reason VARCHAR(500),revision INTEGER NOT NULL DEFAULT 1,updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
INSERT INTO production_jobs(enquiry_id,stage,due_date) SELECT f.enquiry_id,CASE WHEN f.state='PRODUCTION' THEN 'QUEUED' ELSE 'READY' END,q.proposed_date FROM fulfilment_work f JOIN quote_versions q ON q.id=f.quote_id;
CREATE INDEX production_operator_queue ON production_jobs(operator_id,stage,due_date);
ALTER TABLE fulfilment_work ADD COLUMN photos_required BOOLEAN NOT NULL DEFAULT false;
CREATE TABLE delivery_photos(id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES fulfilment_work(enquiry_id),uploaded_by UUID NOT NULL REFERENCES staff_users(id),stage VARCHAR(8) NOT NULL CHECK(stage IN ('PICKUP','TRANSIT','DELIVERY')),caption VARCHAR(500) NOT NULL,display_name VARCHAR(180) NOT NULL,media_type VARCHAR(20) NOT NULL CHECK(media_type IN ('image/png','image/jpeg')),byte_size BIGINT NOT NULL CHECK(byte_size BETWEEN 1 AND 5242880),sha256 CHAR(64) NOT NULL,state VARCHAR(16) NOT NULL CHECK(state IN ('QUARANTINED','CLEAN','REJECTED')),scan_engine VARCHAR(40) NOT NULL,scanned_at TIMESTAMPTZ,created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX delivery_photo_history ON delivery_photos(enquiry_id,stage,created_at);
CREATE TRIGGER delivery_photo_immutable BEFORE UPDATE OR DELETE ON delivery_photos FOR EACH ROW EXECUTE FUNCTION protect_design_artifact();
