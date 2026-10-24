ALTER TABLE design_artifacts ALTER COLUMN uploaded_by DROP NOT NULL;
ALTER TABLE design_events ALTER COLUMN actor_id DROP NOT NULL;
ALTER TABLE mail_outbox ADD COLUMN message_id VARCHAR(180);
ALTER TABLE mail_outbox ADD COLUMN reply_to_id VARCHAR(180);
ALTER TABLE mail_outbox ADD COLUMN subject VARCHAR(240);
CREATE UNIQUE INDEX mail_message_unique ON mail_outbox(message_id) WHERE message_id IS NOT NULL;
