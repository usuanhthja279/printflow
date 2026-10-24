ALTER TABLE payment_receipts DROP CONSTRAINT payment_receipts_method_check;
ALTER TABLE payment_receipts ADD CONSTRAINT payment_receipts_method_check CHECK(method IN ('CASH','UPI','BANK','CARD','CHEQUE','RAZORPAY_TEST'));
CREATE TABLE gateway_orders (
 id UUID PRIMARY KEY, enquiry_id UUID NOT NULL REFERENCES enquiries(id), quote_id UUID NOT NULL REFERENCES quote_versions(id),
 policy_id UUID NOT NULL REFERENCES payment_policies(id), requested_by UUID NOT NULL REFERENCES staff_users(id),
 provider_order VARCHAR(100) NOT NULL UNIQUE, amount_paise BIGINT NOT NULL CHECK(amount_paise>0),
 purpose VARCHAR(12) NOT NULL CHECK(purpose IN ('ADVANCE','BALANCE')), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER gateway_order_immutable BEFORE UPDATE OR DELETE ON gateway_orders FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
CREATE TABLE gateway_captures (
 provider_payment VARCHAR(100) PRIMARY KEY, order_id UUID NOT NULL UNIQUE REFERENCES gateway_orders(id),
 receipt_id UUID NOT NULL UNIQUE REFERENCES payment_receipts(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER gateway_capture_immutable BEFORE UPDATE OR DELETE ON gateway_captures FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
