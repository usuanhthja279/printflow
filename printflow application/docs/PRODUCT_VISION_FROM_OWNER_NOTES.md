# J.D.Printex — product direction from owner notes

Review date: 23 October 2026. Discussion proposal, not an approved scope change or a claim that future features are implemented.

## Source reading

Reviewed all four supplied photographs in filename order. Interpretations below paraphrase readable handwriting rather than claiming a verbatim transcription. A crossed-out word beside the diagram's designer branch is unclear; no requirement is inferred from it.

1. 1000079884.heic: a central workspace connecting customer, designer, inventory database, invoice/payment and social media. Customers may request a quote or sign in; the notes explicitly ask whether quoting should require authentication. Email registration with verification, Google with profile completion, and Microsoft as a later feature.
2. 1000079885.heic: customers submit and track requests, see history, review/comment on design revisions and approve designs. Customer deadline initially optional; staff proposes a timeline and customer confirms it. Agreements already made by phone/email should be recordable without repeating the entire interaction online.
3. 1000079886.heic: define the lifecycle and its stages first. Start an email conversation and synchronize replies/updates with the customer page. Decide how production, payment, invoicing, dispatch and delivery interact and give each participant useful involvement.
4. 1000079887.heic: a polished About section and portfolio; recent work can feed social outreach. Secure payment and invoice generation are important.

## Product thesis

Build an order workspace for a printing business: every enquiry becomes a traceable promise, with the exact specifications, price, proof, delivery commitment and responsible people attached. Customers see progress and the next action they owe; staff see the next action required to move the job safely forward. The owner sees exceptions and opportunities to win repeat business.

The innovative part should be coordination: fewer missed approvals, fewer uncertain promises, fewer repeated explanations, and faster reorders. AI can assist later once the underlying records are reliable.

## Recommended customer journey

Browse real work and product families -> start a quote request -> provide a small set of useful specifications -> save a draft -> authenticate/verify before submitting a formal request -> receive a quote and proposed delivery date -> accept the exact quote version -> review and approve the exact artwork revision -> satisfy agreed payment/release conditions -> production -> quality check -> collection/dispatch -> delivery confirmation -> reorder or feedback.

Keep Request a quote and Sign in as different entry points that meet in the same customer workspace. Prefer allowing product exploration and a local draft before asking for an account; preserve that draft through sign-in and verification. This is a proposal to resolve the question in the notes, not a measured conversion claim. Guest enquiries currently exist; do not silently remove them or break their tracking links. Decide whether to retain guest submission before implementation.

Google can reduce repeated registration input; ask only for missing required profile details after return. Microsoft can remain a later rollout, as the notes suggest. Its current integration scaffold is optional and unconnected, and currently supports an explicit tenant rather than arbitrary personal Microsoft accounts.

## One workspace, several independent tracks

Customer-facing headline stages can remain simple: Request received, Quote ready, Design review, Preparing production, In production, Quality check, Ready for collection / Dispatched, Delivered. Show On hold or Cancelled with an appropriate explanation.

Internally use separate state machines:

- Enquiry: new, reviewing, clarification needed, quoted, converted, lost or cancelled.
- Quote: draft, sent, revision requested, accepted, expired or superseded.
- Artwork: not required, awaiting files, designing, proof shared, changes requested, approved or approval invalidated.
- Production: unreleased, scheduled, material blocked, printing, finishing, QC, rework or complete.
- Payments: not requested, advance due, partially paid, paid, overdue, refund pending or refunded.
- Fulfilment: unallocated, ready, partially dispatched, dispatched, partially delivered, delivered or collected.

The customer headline is derived from these records. A successful payment does not mean a job is printed; a delivered job may still have an approved credit balance. Track operational completion and commercial settlement separately.

For hotel orders with several items, attach proof, quantity, job operations and delivery allocations to order lines. Menus could be ready while leaflets are still printing. A single order-level dropdown would hide this reality.

## Approval and release rules

Customer design approval is necessary but does not automatically start a printing machine. A production release checks the accepted quote version, approved proof revision/specification snapshot, material availability, planned slot and agreed payment terms. An authorized member of staff releases the job.

A change to quantity, paper, finish, artwork or delivery commitment creates an explicit change request. Record price/time impact and ask for the necessary renewed approvals. Preserve earlier accepted records. Never replace an approved file while retaining its old approval badge.

Where design is supplied print-ready or a repeat job needs no new design, support an explicit design-not-required / reuse-approved-artwork path. Do not force every product through unnecessary design work.

When an agreement occurred by phone/email, offer Record customer confirmation: actor, source, time, exact agreed version, reason and evidence/reference. Label it Staff-recorded customer confirmation. The notes suggest bypassing online approval for both parties; the safer product interpretation is delegated recording with audit, not impersonating a customer or fabricating a customer action. Significant proof exceptions require appropriate owner authority and an explicit exception record.

## Deadlines are negotiated commitments

Store three different dates: customer's requested delivery date, staff's proposed commitment and the currently agreed commitment. For wedding jobs optionally capture the event date separately; it is not the production deadline.

A proposed commitment can state conditions such as approval by a cutoff and advance received. A missed dependency raises an at-risk warning and prompts a revised proposal; it must not silently rewrite the agreed date. Keep agreement history. Later, capacity/material data can support availability estimates, with staff confirmation before promises are sent.

## Communication that stays attached to the job

Each workspace needs a customer conversation plus a separate internal activity stream. Customer updates include proof shared, clarification requested, accepted commitment, production milestone and dispatch information. Private cost, vendor terms and internal follow-ups stay private.

Email replies can be synchronized into the same order conversation. Gmail supports thread association using thread IDs, matching subject and reply headers; inbound mailbox changes can be retrieved through the Gmail API. This needs mailbox-read authorization and a synchronization worker, beyond the existing send-only SMTP/outbox adapter.

Use immutable message IDs for deduplication, durable outbound retries and explicit delivery states. A mailbox message must be mapped to an authorized order/participant; a matching email subject alone is not proof of access. Uncertain routing goes to staff triage. Scan attachments before admission, sanitize rendered HTML and avoid email loops. A generic email saying approved should create a staff review task unless it can be tied to an authorized participant and exact revision; it should not silently release production.

Send meaningful customer events, not every staff edit. Offer notification preferences and a daily digest later. The portal remains the durable record even if mailbox threading differs between recipients.

Primary technical sources: https://developers.google.com/workspace/gmail/api/guides/threads and https://developers.google.com/workspace/gmail/api/guides/push . No mailbox was connected or external message sent in this review.

## Payment and documents

Support configurable terms per order: advance plus balance, full prepayment, or approved credit arrangements. Keep the quotation, payment request, payment receipt and final invoice as distinct documents. Invoice timing/tax configuration needs the business's accounting rules confirmed before implementation; delivery should not be the only hard-coded trigger.

Manual bank/cash entries need amount, date, reference, responsible staff and verification status. A customer-uploaded payment screenshot is a claim awaiting verification. For future online payments, use hosted checkout and verified, deduplicated provider events; a browser success screen alone must not mark an order paid. Model refunds and payment allocation to orders so the balance remains explainable.

A possible default is advance before release and balance before dispatch, but this remains an owner policy choice. Customer credit exceptions require authorized recording.

## Useful innovation to prioritize

1. Next-action panel: Waiting for your proof approval, Staff must confirm stock, or Balance required before dispatch, with responsible person and date. Every action should lead directly to the relevant screen.
2. Readiness checklist: show exactly why a job cannot be released. This turns hidden dependencies into manageable work.
3. Version comparison: summarize what changed between proofs/quotes and retain the exact accepted version. Start with structured change notes; visual comparison can follow.
4. Repeat-order assistant: reuse a prior menu, stationery set or leaflet specification as a new draft, then reconfirm current price, quantity and dates. Never automatically reuse an old commercial commitment.
5. Owner exception view: overdue confirmations, date risks, unassigned work, material shortages, jobs awaiting QC and unverified payments. Define these from recorded facts before considering prediction.
6. Consent-controlled portfolio pipeline: a completed order may produce a draft case study or social post only after separate permission and asset selection. Wedding invitations can contain personal information; sanitize or use separately approved photographs. Publishing always begins with staff review.
7. Growth measurement: count enquiries by source, quote turnaround, accepted quotes, repeat orders, lost reasons and delivery performance. Establish a baseline before claiming improvements. Actual job costs and profitability follow reliable material/time capture.

## Role-specific involvement

Customer: clarify requirements, choose options, review price/date, annotate proof, approve and follow delivery. Designer: manage versioned proofs and requested corrections. Sales: qualify requests and coordinate commercial agreement. Production: see only released work and record operation/material/QC events. Accounts: verify receipts and documents. Owner: manage exceptions, permissions, capacity and business outcomes. A customer should never need to see every internal handoff to feel informed.

## Fit with the existing project and development sequence

Already implemented as a development candidate: customer/staff/owner access, owner MFA, enquiry capture and tracking, private staff follow-ups, contacts, product templates, profile/privacy flows and basic mail outbox scaffolding. Existing records do not yet represent a complete order lifecycle. Real provider connectivity and release hardening remain open.

Next slice, Phase 2A: quote versions + requested/proposed/agreed dates + customer acceptance. Deliver a complete path from one enquiry to an accepted quote, with a visible next action. No pricing engine needed initially; staff-entered pricing is enough.

Phase 2B: secure file admission/storage + designer revisions + customer comments + immutable proof approval + production-readiness checklist. First practical pilot: a shaadi card with one artwork revision and a deadline change.

Phase 3: order lines, released production jobs, inventory reservations/issues, finishing, QC/rework, payment ledger/document drafts, collection/partial dispatch and delivery confirmation. Second pilot: a hotel order with menus and leaflets delivered separately.

Phase 4: connected Gmail conversations, real notification delivery, hosted payment provider and reconciliation. Introduce integration boundaries earlier, but do not hold the basic portal workflow hostage to provider setup.

Later: permission-based portfolio publishing, repeat-order campaigns, advanced scheduling/costing and measurable AI assistance. A simple About/portfolio section with owner-approved content can be delivered independently much sooner.

This sequence refines the existing handbook; it does not replace approved phases or change production gates. Native Gradle development remains unchanged.

## Decisions to settle before the next implementation

- Must formal submission require an account, or should verified guest requests remain supported?
- What advance/balance/credit terms does J.D.Printex actually use?
- Who may record an offline customer agreement and authorize exceptional production release?
- Is the committed date a collection/dispatch date or delivery-at-customer date?
- Which designer/production tasks happen in-house and which are outsourced?

These questions do not block a product discussion. They do affect transaction rules, permissions and acceptance checks before implementation.

## Assessment and decision

Concept rating: 8/10 (subjective). Strong operational fit; growth outcomes remain unmeasured. Keep collaboration and traceable commitments, add exception management and repeat-order drafts, and defer AI/social automation/advanced scheduling. Avoid implicit approval and payment rules. Owner authorized Phase 2 development; first slice is immutable quotations with customer acceptance, as specified in DEVELOPMENT_HANDBOOK.md.
