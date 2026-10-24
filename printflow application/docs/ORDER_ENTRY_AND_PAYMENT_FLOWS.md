# Customer entry, notifications and payment workflow — 24 October 2026

This records the owner's proposed flow alongside implementation status. The active development slice remains in-app multipage PDF proof review and a dedicated local test designer. Features described as planned are not represented as already connected.

## One order, four entry paths

All paths converge on the same versioned enquiry, quotation, accepted live work, artwork review, production, delivery and completion record. A customer account is access to that record; registration must not create a duplicate order.

1. Guest online/app: submit contact/specification details and optional reference artwork. Review intake, accept/reject with a customer-visible explanation, and provision guest access at acceptance. Send temporary credentials only to the supplied new customer email; possession plus required password change/consent activates the account. Send the quotation PDF and workspace link; request artwork if absent. Customer accepts that exact offer or requests a revision.
2. Guest phone/email: staff records the same enquiry with its source, requested date, contact and a conversation summary. Record explicit phone/email agreement evidence if already accepted. Never turn a price discussion into approval. Current native portal access requires an email address; phone-only offline handling and a consented WhatsApp invitation path are planned, not a reason to invent an email address.
3. Registered online/app: prefill the customer's saved contact details, retain their existing sign-in and offer their own earlier artwork plus approved public samples. Do not send another password or create another account.
4. Registered phone/email: staff links the enquiry to the confirmed existing customer identity and records source/notes. Keep the same portal, quote and proof decisions; do not trust a matching phone number alone as authority to access someone else's account.

Intake role decision to reconcile: current permissions are owner/designer intake review, scoped assigned sales, per the earlier owner instruction. The latest proposal adds owner/sales review. Implement that as an explicit owner-granted intake permission and matching UI/API scope rather than opening every enquiry to every staff account. This permission change is planned after the PDF slice.

## Shared fulfilment

After quotation acceptance (portal or evidenced offline agreement), work enters Live works. Planned automatic designer assignment should consider enabled DESIGNER accounts, availability, workload and relevant print/design skill, with an owner override and an unassigned queue if nobody qualifies. Retain a chosen designer across revisions. Current assignment is explicit by the owner; no automatic workload selection is claimed.

Customer chooses a reference or requests design help. The designer receives the accepted brief and reference comments, uploads a clean private draft and shares a numbered proof. Customer views pages in-app, pins location comments tied to revision/page/normalized coordinates and requests changes. Comments request edits; the original source file is never silently overwritten. Replacement revisions preserve previous comments/decisions and require fresh full-document approval.

Production requires the exact current quote/proof approval and owner release. Planned finance/material gates must be explicit; do not infer payment from a proof checkbox. Production/QC milestones lead to dispatch and delivery. Pickup/drop-off scanned private photos and optional consented GPS are planned. Confirmed delivery starts the 48-hour review period; an issue pauses completion and resolution restarts review.

## Safe sample reuse

There are two separate sources: the customer's own accessible earlier artwork and an owner-published sample gallery. Other customers' private projects are never a gallery by default. Publication requires recorded permission/rights, removal of names, addresses, phone numbers and other personal details, and an approved sanitized preview. Selecting a sample creates a new work reference with sample/version attribution; it does not grant access to the original customer's files, quote, comments or history. Gallery publication/reuse is planned.

## Notification checkpoints

- Enquiry received: customer acknowledgement and team intake alert.
- Intake accepted/rejected: customer decision; new customer access instructions only when needed. Existing credentials remain unchanged.
- Quotation issued/revised: attach the exact issued PDF, scope/total/delivery summary, current workspace link, artwork request if absent, and explicit waiting-for-approval or evidenced-already-accepted state.
- Quotation accepted/change requested: team/assigned staff alert and customer confirmation. A new quote is an immutable version; old offers stay in history.
- Artwork requested: customer action request. A supplied reference acknowledgement can be in-app; avoid duplicate requests.
- Proof shared: customer review action. Changes requested and final proof approval notify the designer/owner, with customer confirmation. Planned comment batching/digests should group pins rather than emailing every click; current saved comments generate individual notifications.
- Payment requested/recorded/overdue: planned customer action/receipt and owner confirmation, tied to the invoice/payment allocation rather than a manually asserted paid state.
- Production start: customer confirmation. Finishing/QC updates are in-app by default, with an email for ready-for-dispatch when relevant.
- Dispatch: customer delivery summary and assigned staff contact; planned evidence links must remain authenticated.
- Delivered: customer confirmation, evidence when implemented and review deadline. Issue/resolve notifications go to owner/assigned staff. Completed closes the review period and invites a repeat order.

Email is configured and tested. WhatsApp requires official provider setup, recipient consent, approved templates where required and delivery status/webhook checks; it is currently not connected. Use channel preferences and deduplication so simultaneous email/WhatsApp do not create conflicting instructions. Incoming email synchronization remains planned.

## Money and documents — proposed owner-configurable policy

A quotation is the commercial offer, not proof of payment or a tax invoice. After quotation acceptance, create a payment schedule/payment request or pro forma summary. An upfront design fee can be an explicit quote item for design-heavy work; otherwise request the agreed deposit after acceptance. No fixed percentage is assumed. Allow deposit amount/percentage, full advance, approved credit terms and recorded owner exceptions per customer/order.

Record a receipt when money is actually received and verified. The planned production gate is: exact approved proof + accepted scope + required deposit confirmed (or approved credit/waiver) + operational readiness. The usual balance request belongs before dispatch; trusted credit customers may have a recorded due date. Delivery/completion does not automatically mean money is settled.

Tax invoice timing is a separate statutory configuration. CGST section 31(1) says a GST-registered supplier of taxable goods issues the invoice before/at removal for moving goods, or delivery/making available otherwise. Section 31(3)(d) addresses advance receipt documentation. Printing goods/service classification, GST registration, exemptions and applicable rules must be configured with the business's accountant; do not hard-code a universal end-of-delivery invoice trigger or tax rate. Draft/pro forma documents must be labelled distinctly, and issued financial records require immutable numbering, allocations, audit history and appropriate credit/refund notes rather than edits.

Primary reference checked 24 October 2026: [CBIC CGST Act, section 31](https://taxinformation.cbic.gov.in/content-page/explore-act/1000300/1000001/invoice/ACTS). Payment/invoicing/credit enforcement is planned; the current manual production milestones do not implement it.

## Implementation order

1. Finish and verify multipage PDF preview, page pins, full-document approval and persistent local test designer.
2. Add sample consent/publication and secure own-artwork reuse; then automatic designer assignment and owner-delegated sales intake scope.
3. Add payment schedule, verified receipts, financial documents and explicit production/dispatch payment gates.
4. Add phone-friendly private pickup/delivery evidence, optional GPS and official WhatsApp delivery.


24 October implementation update: the manual verified payment schedule/receipt, commercial invoice PDF, production/dispatch finance gates and Razorpay test adapter described in PAYMENTS_AND_INVOICES.md supersede the planned-payment statements above for this bounded slice. GST tax invoices, live gateway settlement/refund handling and partial dispatch remain planned.
