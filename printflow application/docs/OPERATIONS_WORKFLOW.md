# Designer routing, production queues and delivery photos

24 October 2026. Owner-authorized local development. Java 21, checked-in Gradle wrapper, PostgreSQL and Next.js; no Docker. This is a project-level operations slice, not a full inventory or machine-capacity system.

## Designer assignment

Customer quotation acceptance and evidenced offline acceptance trigger assignment within the acceptance transaction. Enabled, available DESIGNER accounts that completed password setup are eligible. Choose the fewest active design-stage enquiries, then the oldest automatic assignment/creation time and UUID. Production-released work stops counting toward design workload. A transaction advisory lock serializes automatic selections; the enquiry lock prevents duplicate assignment.

Existing assigned designers are preserved. Manual owner assignment, including deliberately leaving work unassigned, sets MANUAL mode. “Use automatic assignment” restores AUTO mode and tries unassigned accepted work immediately. Pending AUTO enquiries retry every minute, in batches of 25. Disabled/unavailable designers get no new automatic work; existing assignments require owner review rather than silently moving to someone else.

Owner Design Routing shows workload and availability pause/resume. Availability changes are audited. Public designer names are used in customer notices; staff login addresses are not exposed in those notices.

Reserved `.test` customer addresses route only to the TEST designer pool; ordinary enquiries use BUSINESS. Browser fixtures create a TEST designer. This prevents synthetic testing from assigning real enquiries to temporary accounts or notifying real designers. The fixture cleanup disables synthetic accounts and archives their enquiries. Routing pools are not editable by customers.

## Production

The owner still releases production only after an accepted quotation, exact clean approved proof, current finance agreement and required verified advance. Release creates a QUEUED production job bound through fulfilment to that quotation/proof.

Queue: QUEUED → PRINTING → FINISHING → QC → READY. QC can return to PRINTING for rework. HOLD preserves the stage and blocks advancement until a reasoned RESUME. Every operation uses an optimistic revision, explicit confirmation and customer-visible note. Passing QC also marks fulfilment ready for dispatch; the former direct Ready action cannot skip queue operations.

Owner assigns an enabled printer, a descriptive machine/press label, NORMAL/URGENT priority and due date. The machine label is not a capacity reservation. Owner sees the latest 200 visible active accepted projects, including awaiting-release work. Printers see only jobs assigned to their account. Search uses product, public reference, printer and machine; filters highlight holds and urgency. Completed production remains in Ready/delivery while the project is active.

Production queue access does not grant quotation prices, customer contact lists, customer source artwork or owner finance authority to a printer. Existing assigned specialist tasks remain separate from operational milestones. Delivery assignment uses the existing owner staff-job mechanism.

## Delivery evidence

Owner or assigned delivery staff upload PNG/JPEG photos for PICKUP, TRANSIT or DELIVERY. Limit 5 MiB/photo, 30 photos/project and a bounded total. Exact MIME/signature/filename admission, actor quotas, native malware scanning, rollback cleanup, private UUID storage and verified SHA-256 reads reuse the established private-file boundary. No public image URLs are created.

New releases require at least one clean pickup photo before dispatch and one clean delivery photo before confirming delivery. Both reads check file integrity. Transit photos are optional. Legacy fulfilment rows retain their previous optional-photo policy. Before dispatch, the owner can record a customer-visible reason to waive or restore the evidence requirement; changing it increments the fulfilment revision and records an event/notification.

Pickup uploads open when ready; transit uploads during transit; delivery uploads during transit or delivered review. Scanning failures stay quarantined and may be retried by authorized staff. Customers see only clean photos. Photo bytes, stage, caption, author and upload time are immutable; scanning metadata alone can change. No deletion/replacement operation is provided.

Customers and current assigned team members can preview only their project's evidence. Operators may read evidence for their assigned production job but cannot upload or change evidence policy. Closed work is read-only. Server times represent upload time, not verified capture location/time. Original evidence files are preserved; GPS extraction/tracking is not implemented.

## Notifications and views

- DESIGN_ASSIGN: automatic assignment confirmation for customer and scoped team.
- PRODUCTION_UPDATE: operation, hold/resume and rework updates; assignment notes go to the team, including the newly assigned printer.
- DELIVERY_PHOTO: a clean photo is available in the authenticated project workspace, or an owner policy exception was recorded. Images are not attached to email.
- Existing READY, TRANSIT and DELIVERED notifications remain on the order thread.

Views: owner Design Routing; owner/printer Production board; production assignment/operation dialog; assigned delivery task; pickup/transit/delivery upload and gallery; customer full-photo preview; final work overview and outgoing email activity. The browser evidence report describes each captured view and its purpose for redesign.

## API and schema

- GET /api/v1/owner/design-routing; PATCH /api/v1/owner/design-routing/{designer}: `{available}`.
- Existing POST /api/v1/design/enquiries/{id}/designer accepts `{designerId}` for manual control or `{automatic:true}`. Design workspace adds designerAssignmentMode.
- GET /api/v1/production/queue; GET/POST /api/v1/production/enquiries/{id}. POST: revision, action ASSIGN/PRINTING/FINISHING/QC/REWORK/READY/HOLD/RESUME, note, confirmed:true; assignment optionally adds operatorId, machine, priority, dueDate.
- GET/POST /api/v1/design/enquiries/{id}/fulfilment/photos. Upload is raw image bytes, `?stage=`, Content-Type, percent-encoded X-File-Name and X-Photo-Caption, plus normal session CSRF.
- POST .../photos/{photo}/scan; GET .../photos/{photo}/preview.
- POST .../photos/policy: required, note, confirmed:true; owner only before dispatch.

All responses are no-store. Existing current-session, password-change, owner-MFA and per-resource scopes remain. Only the exact photo-upload POST has a 5 MiB body allowance; photo policy/scan JSON retains the normal limit. V24 adds routing/queues/photo identity and legacy-job backfill. V25 gives delivery photos their own scan-metadata-only mutation guard, independent of artwork PDF metadata.

## Focused verification

Run the one integration case `WorkflowMailIntegrationTest.routingProductionQueueAndDeliveryPhotosRespectScopesAndGates` with Gradle. It exercises acceptance routing/idempotency/manual override, production authorization/order/hold/rework/QC, quarantined evidence rejection/rescan and customer/stranger preview scopes. Its scanner is a test double; the browser walkthrough uses the configured native scanner.

Run `./scripts/Test-WorkflowMailBrowser.ps1 -Check operations -Project desktop` against a native `-DisableMail` startup. One journey covers guest/account/quotation/proof, automatic assignment, manually verified advance/balance, invoice preview, printer queue/hold/rework/QC and delivery evidence through customer review. Fictional image fixtures are generated in the browser and clearly labelled SYNTHETIC TEST. Real SMTP, live money and GPS are not exercised. Final results and screenshots are recorded in the evidence pack, not inferred from implementation.

The operations walkthrough also corrected a stale owner-stage refresh: Refresh current stage now refreshes the enquiry projection, and accepted work does not hide staff assignment based on an old queue status.

## Recorded result

The final focused Gradle operations case passed, including assigned-printer notification scope. The frontend production build/TypeScript passed. The desktop operations browser journey passed in 35.0 seconds (37.3-second runner), captured 40 indexed views including three narrow-screen views, and cleaned synthetic accounts/unsent mail. Earlier diagnostic attempts corrected the fixture working directory, dropdown/retained-textarea selectors and a case-sensitive test assertion; those failed attempts are not reported as passes. The frontend owner-stage refresh correction is included in the final build. No broad suite or separate full-device mobile run was performed.

The ZIP report records each screenshot’s significance and concrete redesign observations: compact the owner operations header, improve phone lane navigation, review gallery cropping and place required delivery evidence beside its milestone action.
