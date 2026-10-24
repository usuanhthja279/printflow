ALTER TABLE staff_users ADD COLUMN contacts_access BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE enquiries ADD COLUMN intake_accepted_by UUID REFERENCES staff_users(id);
ALTER TABLE enquiries ADD COLUMN intake_accepted_at TIMESTAMPTZ;
-- Preserve historically issued agreements while introducing an explicit intake gate for new work.
UPDATE enquiries e SET status=CASE WHEN EXISTS(SELECT 1 FROM quote_versions q WHERE q.enquiry_id=e.id AND q.state='ACCEPTED') THEN 'IN_PROGRESS' ELSE 'REVIEWING' END,revision=revision+1 WHERE e.status='NEW' AND EXISTS(SELECT 1 FROM quote_versions q WHERE q.enquiry_id=e.id);
ALTER TABLE mail_outbox ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE mail_outbox ADD COLUMN enquiry_id UUID REFERENCES enquiries(id);
CREATE UNIQUE INDEX one_rejection_email ON mail_outbox(enquiry_id) WHERE kind='REJECTION';
CREATE TABLE staff_job_tasks (
 id UUID PRIMARY KEY,enquiry_id UUID NOT NULL REFERENCES enquiries(id),staff_id UUID NOT NULL REFERENCES staff_users(id),
 job_role VARCHAR(32) NOT NULL,title VARCHAR(200) NOT NULL,notes VARCHAR(2000) NOT NULL,due_date DATE NOT NULL,
 state VARCHAR(16) NOT NULL DEFAULT 'TODO' CHECK(state IN ('TODO','IN_PROGRESS','DONE')),revision INTEGER NOT NULL DEFAULT 1,
 assigned_by UUID NOT NULL REFERENCES staff_users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX staff_job_queue ON staff_job_tasks(staff_id,state,due_date);
CREATE TABLE staff_job_events(id UUID PRIMARY KEY,job_id UUID NOT NULL REFERENCES staff_job_tasks(id),actor_id UUID NOT NULL REFERENCES staff_users(id),action VARCHAR(40) NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TRIGGER staff_job_event_immutable BEFORE UPDATE OR DELETE ON staff_job_events FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
