# Payments, invoices and Razorpay test checkout

Owner-authorized PF-320 slice, 24 October 2026. Native Java 21/Gradle/PostgreSQL/Next.js; no Docker. Production deployment and live gateway payments remain unauthorized.

## Workflow

After quotation acceptance the owner configures an explicit advance amount in paise. No deposit percentage is assumed. Zero advance is an explicit owner agreement with a customer-visible reason. The balance is required before dispatch unless the owner records a credit due date and reason. Policies are versioned, immutable and tied to the accepted quotation. New accepted amendments require new terms; payments remain allocated to the same enquiry account with their original quotation attribution.

Owner-confirmed cash, bank, UPI, card and cleared-cheque payments create immutable numbered receipts. Customers and scoped sales can read, but cannot mark themselves paid. Owner receipt corrections append a reasoned reversal; they never erase the original or execute a bank refund. Duplicate request IDs are idempotent only for identical details; duplicate references and payments above the balance are rejected. Corrections cannot undermine released production or dispatched payment gates.

Production release requires the existing exact clean approved proof and accepted quotation, plus current configured finance terms and verified advance. Dispatch additionally requires the current invoice and either full balance or unexpired approved credit. Overpayments require owner reconciliation. Old work without terms must be configured rather than implicitly bypassed. Due dates use Asia/Kolkata. Completion does not mark unpaid credit as paid; owner receipts can settle closed work.

Invoice issue creates one immutable numbered commercial invoice per accepted quotation. It snapshots customer, scope, item pricing/tax, terms and payment agreement, stores PDF bytes/hash, and queues customer/team PDF notifications within the same transaction. Preview/download require existing customer/assigned-sales/owner scope and hash validation. Original invoices and payment rules are locked after issue. Invoiced quotation changes require a future credit-note workflow rather than creating conflicting invoices. PDFs are explicitly commercial invoices, not GST tax invoices. Statutory GST invoice configuration, financial-year numbering, tax IDs/classification, credit/refund notes, settlements and partial dispatch are separate remaining work.

## Razorpay test mode

Both manual receipts and Razorpay test payments feed the same ledger. This adapter accepts only rzp_test_ keys outside prod. Live keys are never enabled by this slice; test receipts do not count toward production-profile balances. The UI says TEST and explains that no real money moves. Configured credentials are not represented as verified live provider connectivity.

Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET privately in the process/system environment. Restart the application/parent terminal after changing Windows variables. Optional RAZORPAY_WEBHOOK_SECRET is a separately chosen private webhook secret. No credentials belong in source, browser logs or chat. Startup inherits the parent environment and imports these Windows user variables when the existing parent session predates the update. Manual payments remain usable when gateway configuration is absent.

Customer checkout creates/reuses a server-owned order for the precise advance shortfall or balance. The browser cannot supply the payable amount. The server binds enquiry, accepted quote, payment policy and requesting customer; verifies callback HMAC; then fetches the payment from the fixed official API and requires captured state, matching order/currency/amount and no refund. A checkout success screen alone never credits the ledger. One capture per order/payment and enquiry row locks deduplicate callback/webhook races. Captured funds arriving against a subsequently changed agreement remain recorded and may show an overpayment hold instead of silently dropping money. This is not settlement reconciliation.

The signed payment.captured webhook uses a bounded raw body and its own HMAC; only that exact route is exempt from CSRF. Customer checkout/verify routes retain sessions and CSRF. Webhooks also reconcile payment data via the provider API. Public webhook delivery requires a publicly reachable URL; no tunnel/deployment is created automatically. Local checkout uses server verification without needing inbound webhooks. Refund/chargeback synchronization is not implemented; this adapter must remain test-only until those controls and live verification are completed.

Official references checked 24 October 2026:
- https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/integration-steps/
- https://razorpay.com/docs/api/orders/create/
- https://razorpay.com/docs/api/payments/fetch-with-id/
- https://razorpay.com/docs/payments/dashboard/account-settings/api-keys/
- https://razorpay.com/docs/webhooks/setup-edit-payments/

## API

Base: /api/v1/design/enquiries/{id}/payments. All responses no-store.
- GET: current accepted total, paid/balance/overpaid, rules, receipts, invoice metadata, stage and actor capabilities.
- POST /policy: quoteId, expectedRevision, advancePaise, optional creditDue, reason. Owner only, before release/invoice.
- POST /receipts: id (retry UUID), quoteId, amountPaise, method, reference, paidOn, verified:true. Owner only.
- POST /receipts/{receipt}/reverse: reason. Owner only, gate-preserving correction.
- POST /invoices: quoteId. Owner only, idempotent exact accepted agreement.
- GET /invoices/{invoice}/document: scoped inline authenticated PDF.
- GET /gateway: configuration flags; no secrets, no live-enabled claim.
- POST /gateway/orders: quoteId, purpose:ADVANCE|BALANCE. Linked customer only.
- POST /gateway/verify: orderId, paymentId, signature. Linked customer only.
- POST /api/public/payments/razorpay/webhook: raw signed JSON, max64KiB. Separate webhook HMAC; payment.captured only.

V22 adds immutable policies/receipts/reversals/invoices and invoice mail binding. V23 adds immutable test gateway orders/captures. Money uses integer paise and existing quote limits; no floating-point amount arithmetic on the server.

## Focused verification

InvoicePdfTest verifies document amounts, pagination, rendering and signature tampering. QuotationMailTest verifies exact immutable invoice attachment and hash failure. WorkflowMailIntegrationTest checks real HTTP sessions/owner MFA/CSRF, actor scopes, duplicate/overpaid/uncleared receipts, corrections, invoice immutability, advance-before-production and balance-before-dispatch, plus mocked provider callback/webhook idempotency. The existing workflow browser journey adds owner payment rules, split receipts and customer invoice preview before delivery. Synthetic SMTP is disabled throughout testing. Real provider checkout cannot be claimed until test credentials are configured and a provider test transaction is verified.


### Verified outcome

Eight focused unit tests passed (InvoicePdfTest: 2; QuotationMailTest: 6). Eight native PostgreSQL WorkflowMailIntegrationTest cases passed; an additional focused rerun of the credit rule case passed with valid-credit acceptance and expired-credit rejection. Frontend production build/type checks passed. The desktop guest/account → quote/proof → advance → invoice preview → balance → production/QC → dispatch/delivery UI test passed in 22.7 seconds (24.8-second runner), including creation of an actual Razorpay TEST order for the server-calculated advance. No real SMTP or money movement occurred during the synthetic journey. The initial final-email assertion exceeded the latest-20 preview window after finance emails were added; it was moved to the point of comment creation and passed without application changes.

Razorpay test credentials were saved privately in Windows user environment variables at the owner's request. An authenticated read-only official Orders API check returned HTTP 200, and the app-created test order matched the expected amount/currency. No actual captured provider payment or captured public webhook delivery was verified; captured verification/signature rejection/callback-webhook deduplication are covered with mocked provider responses and real HTTP/database tests. The unpaid remote test order remains in the provider sandbox; local synthetic accounts and unsent mail payloads were cleaned. A separate webhook secret and path-restricted temporary HTTPS endpoint were subsequently configured; the user confirmed the Dashboard entry. Actual captured-provider webhook delivery remains unverified. The latest browser attempt stopped at test-bank selection when its Pay-button selector timed out. Further retries stopped at the owner’s request for minimum testing. Test checkout remains distinct from live gateway activation.

Invoice PNG rendering and the authenticated in-app invoice preview were inspected. The standard native app is restored after testing; final health/mail checks are recorded separately when restoration finishes.

Final native restoration verified: API UP, customer UI HTTP 200, normal email delivery enabled. Windows user test credentials remain saved; no secrets were included in repository files or verification output.
