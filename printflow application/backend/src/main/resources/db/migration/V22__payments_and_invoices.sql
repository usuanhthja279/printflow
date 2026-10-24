CREATE TABLE payment_policies (
 id UUID PRIMARY KEY, enquiry_id UUID NOT NULL REFERENCES enquiries(id), quote_id UUID NOT NULL REFERENCES quote_versions(id),
 revision INTEGER NOT NULL CHECK(revision>0), advance_paise BIGINT NOT NULL CHECK(advance_paise>=0),
 credit_due DATE, reason VARCHAR(1000) NOT NULL CHECK(length(trim(reason))>=10),
 actor_id UUID NOT NULL REFERENCES staff_users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(enquiry_id,revision)
);
CREATE TRIGGER payment_policy_immutable BEFORE UPDATE OR DELETE ON payment_policies FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TABLE payment_receipts (
 id UUID PRIMARY KEY, enquiry_id UUID NOT NULL REFERENCES enquiries(id), quote_id UUID NOT NULL REFERENCES quote_versions(id),
 receipt_number BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,
 amount_paise BIGINT NOT NULL CHECK(amount_paise BETWEEN 1 AND 100000000000),
 method VARCHAR(16) NOT NULL CHECK(method IN ('CASH','UPI','BANK','CARD','CHEQUE')),
 reference VARCHAR(200) NOT NULL CHECK(length(trim(reference))>=3), paid_on DATE NOT NULL,
 actor_id UUID NOT NULL REFERENCES staff_users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(enquiry_id,method,reference)
);
CREATE TRIGGER payment_receipt_immutable BEFORE UPDATE OR DELETE ON payment_receipts FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TABLE payment_reversals (
 id UUID PRIMARY KEY, receipt_id UUID NOT NULL UNIQUE REFERENCES payment_receipts(id),
 reason VARCHAR(1000) NOT NULL CHECK(length(trim(reason))>=10), actor_id UUID NOT NULL REFERENCES staff_users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER payment_reversal_immutable BEFORE UPDATE OR DELETE ON payment_reversals FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TABLE finance_invoices (
 id UUID PRIMARY KEY, enquiry_id UUID NOT NULL REFERENCES enquiries(id), quote_id UUID NOT NULL UNIQUE REFERENCES quote_versions(id),
 invoice_number BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE, issued_on DATE NOT NULL,
 total_paise BIGINT NOT NULL CHECK(total_paise>0), snapshot JSONB NOT NULL,
 document_pdf BYTEA NOT NULL, document_sha256 CHAR(64) NOT NULL,
 actor_id UUID NOT NULL REFERENCES staff_users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER finance_invoice_immutable BEFORE UPDATE OR DELETE ON finance_invoices FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
ALTER TABLE mail_outbox ADD COLUMN invoice_id UUID REFERENCES finance_invoices(id);

ALTER TABLE work_events ALTER COLUMN message TYPE VARCHAR(2000);
