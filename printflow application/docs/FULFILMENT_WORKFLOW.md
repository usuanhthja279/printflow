# J.D.Printex — one-stage-at-a-time fulfilment

Owner direction, 24 October 2026. This supersedes the all-modules enquiry popup. Keep native Gradle development; no Docker. The lifecycle below is the product specification, not a claim that production, delivery or mobile tracking are already implemented.

## Interaction rule

One work record, one current stage, one primary action. Show the job name, responsible person, promised date and next action first. Completed stages become single-line history entries with expandable detail; future stage controls stay hidden. A role sees only its relevant queue and the fields required for its task. Hiding a screen is supplemented by backend authorization.

Separate **Incoming enquiries** from **Live works** in both customer and business navigation. Quotation acceptance moves the project to Live works automatically; no user should re-enter its specifications. The commercial order record must preserve the accepted item/price/specification references. An amendment requires new acceptance and, where relevant, new proof review.

## 1. Incoming enquiry — owner or designer

Show the customer's print requirements, quantity, requested date and supplied references. Owner may select an owner/designer reviewer; designer may take an available intake for themselves. Accept or reject, with an optional comment. Missing rejection comments use a standard meaningful explanation and invite a phone discussion. Accepting intake is not accepting a quotation.

Sales cannot see unaccepted intake, including through direct API calls. Printer/delivery staff cannot open this stage. Once decided, replace this view with an expandable history line: actor, time, assignment, decision and comment. A rejection remains visible to the linked customer and records the notification state truthfully.

## 2. Quotation — owner or explicitly delegated commercial staff

Show only specifications, item pricing, entered tax, commitment date and terms, with Save draft and Send quotation. Keep previous versions collapsed. No quotation expiry, following the owner's earlier decision.

If the customer has **already accepted** by phone/email, record the channel and agreement evidence and move to Live works when sent. Merely discussing a price is not consent. Otherwise show Waiting for customer approval and let the customer accept that exact version or request a change. Preserve the agreed version, actor/time and document hash. No extra manual status selection.

## 3. Live work — artwork preparation and review

Use small substages: **Artwork required → Designer working → Awaiting customer review → Changes requested → Approved**. Show only the current action. If the customer already supplied artwork, skip the request step. Otherwise the assigned designer sends an artwork request into the project's conversation/notification channel.

Designer uploads a private scanned draft and explicitly sends a numbered proof for review. The customer opens it inside Live works; no manual download should be required for supported preview formats. Multi-page PDF review needs page thumbnails/navigation, zoom and location comments tied to the exact revision, page number and normalized coordinates. Render only needed pages with bounded resources; use the authenticated clean file and an isolated PDF worker. Do not enable embedded PDF scripts or load external document resources.

Customer annotations request changes from the designer; they do not silently edit the source file. A replacement revision preserves old comments/decisions and needs fresh approval. Show unresolved requests to the designer, with a resolution explanation linked to the replacement. Approval acknowledges the whole exact document, not just the currently visible page. Old approval links cannot approve a replacement or an amended quotation.

Current implementation has inline PNG/JPEG review/pins and private PDF download review. Safe multi-page in-app PDF rendering and page-aware review acknowledgement are the next viewer slice; they must not be represented as already available.

## 4. In production — assigned printer and owner

After exact proof approval, automatically place work in the **production queue**. The public stage may read In production with a clear substatus such as Waiting to start, Printing, Finishing or Quality check; do not claim a press has started just because a proof was approved.

Owner assigns the printer; printer sees only the relevant accepted specifications, artwork and production instructions. Starting actual manufacture records the responsible operator and time. Add a small internal quantity/quality check before transit. Keep materials, payments/credit policy and exceptions inside staff controls, not as extra customer forms. Do not require online payment for every customer without an owner-approved business rule.

An approved-artwork change after production starts becomes an explicit hold/rework/change agreement; never silently replace the file under a running job. Formal machine release, material/QC and manufacturing state controls belong to Phase 3; current generic staff tasks do not implement those controls.

## 5. In transit — assigned delivery staff

Owner assigns one delivery person. Their phone-friendly page shows only pickup/drop-off details and the next required check:

1. Collect from source: capture/upload a pickup photo and confirm collection.
2. Start transit: record server time and the assigned delivery actor.
3. Deliver: capture/upload a delivery photo and confirm handover, with an exception action for failed delivery.

Store evidence privately with type/size/scan controls and immutable actor/time/job linkage. Phone capture may use the camera picker; retry/offline upload must be idempotent and show unsynced evidence honestly. Photo metadata must not grant authorization or be treated as a trusted timestamp.

GPS is optional and requires explicit device permission. Begin with location recorded at pickup/delivery, including accuracy and capture/upload times. A browser page cannot promise reliable continuous background tracking. A later native/PWA delivery slice can explore suitable device capabilities; declining GPS must leave a usable evidence-based delivery process. Do not expose a staff member's unrelated or off-duty location.

## 6. Delivered — 48-hour review

Confirmed delivery starts the server-side 48-hour review window. Show Delivered, under review, the completion time and a simple Report an issue action. A customer issue creates an owner/assigned-staff notification and pauses automatic completion. Failed delivery or missing required evidence cannot start the timer.

## 7. Completed

A durable, idempotent scheduled transition completes the work after 48 hours only when delivery is confirmed and no issue/hold is open. The timer must survive service restart and use stored server timestamps, not a browser countdown. Preserve proof, delivery evidence and agreement history according to retention policy. Completion is an operational state, not an automatic claim that payment has been settled.

## Status requests and notification rules

During production/transit, the customer may request an update. Persist the request, notify both owner and the current stage's assigned staff, and let staff respond into the project history. Keep one unresolved request per work item or apply a visible cooldown to avoid notification spam. Never send private staff notes to the customer. In-app alerts and email delivery have distinct states; email/WhatsApp are connected only after provider setup and a successful delivery check. No automatic phone call is implied.

## Delivery order for implementation

1. Current review fixes: role queues, owner-granted Contacts, intake decision/assignment/comments, owner queue archival/standard email, and current-stage UI with collapsed past views.
2. Complete the focused viewer/communication slice: multi-page PDF preview, page comments, explicit artwork requests and assigned in-app notifications/status requests.
3. Phase 3: automatic commercial conversion/production queue, printer execution and internal quality handoff.
4. Phone-first delivery: assigned delivery workflow, scanned private photos and optional consent-based location.
5. Confirmed delivery review window, issue hold, restart-safe completion timer and end-to-end fulfilment verification.

The current build must keep unimplemented production/transit/review-timer controls unavailable rather than inventing progress. Owner review and focused bug fixes precede the next implementation slice.

24 October lifecycle-mail continuation: V13 adds one-time customer setup with quotation, lifecycle customer/team notifications, owner Email activity, proof-gated manually confirmed production/QC/dispatch/delivery milestones, delivery issue holds and durable 48-hour completion. This supersedes earlier “milestone emails/manual operations/timer not implemented” notes only for this bounded slice. Photos/GPS, stock/payment production controls, inbound Gmail sync and multi-page artwork viewer remain pending. Scope, APIs and verification: WORKFLOW_EMAIL_VERIFICATION.md.

24 October PDF review slice: native resource-limited PDF inspection and authenticated local-worker multipage canvas preview, per-page location comments, full-document page acknowledgements tied to immutable revision/hash/quote, reference/file Preview dialogs and a persistent least-privilege local test designer are implemented. This supersedes earlier multipage PDF viewer pending notes only for PDF/image preview/comment review; source editing, advanced preflight, gallery reuse, delivery photos/GPS and payment/WhatsApp integration remain separate. API, migration, role and focused evidence: PDF_ARTWORK_REVIEW.md. Owner-proposed entry/payment/notification paths: ORDER_ENTRY_AND_PAYMENT_FLOWS.md.
