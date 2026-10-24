ALTER TABLE mail_outbox ADD COLUMN quote_id UUID REFERENCES quote_versions(id);
-- Bind previously queued quotation messages to the version identified at issue, never the latest version.
UPDATE mail_outbox m SET quote_id=q.id FROM quote_versions q
WHERE m.kind='QUOTE' AND m.enquiry_id=q.enquiry_id AND m.message_id='<quote-'||q.id::text||'@jdprintex.in>';
