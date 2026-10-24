-- Previously sent messages remain in their original thread. Only new project events use phases.
ALTER TABLE mail_outbox ADD COLUMN thread_phase VARCHAR(8) NOT NULL DEFAULT 'LEGACY' CHECK (thread_phase IN ('LEGACY','INTAKE','WORK'));
ALTER TABLE mail_outbox ADD COLUMN thread_root_id VARCHAR(180);
CREATE INDEX mail_phase_recipient ON mail_outbox(enquiry_id,thread_phase,lower(recipient),created_at DESC);
