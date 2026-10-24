# Screen inventory — customer, business, operator, vendor

## Implemented screens — foundation and first staff slice

23 October 2026: customer pages now use J.D.Printex branding and owner-confirmed service categories. Homepage service links pass `?product=CODE` to `/request-quote`; known codes preselect the enquiry product. Unknown codes retain the default. Paper weight and size default to unspecified rather than assuming brochure stock. The existing local-only boundary remains.
| Route | Role | Outcome | Status |
|---|---|---|---|
| `/` | Public | Marketing/product overview and navigation | Built |
| `/request-quote` | Public | Working data submission to backend | Built (local only) |
| `/track` | Token holder | Enquiry status + event timeline | Built |
| `/business` | SALES / MFA-verified OWNER | Session login, scoped enquiry queue, manual intake, assignment, follow-up, status and internal activity | Built first Phase 1 slice (local) |

## Planned: customer portal
| Screen ID | Route idea | Job-to-be-done | Phase |
|---|---|---|---|
| CU-01 | `/customer/dashboard` | Current orders, approvals and outstanding balance | 1–3 |
| CU-02 | `/products/[slug]` | Product photos, options, examples, use cases | 1 |
| CU-03 | `/quote/new` | Guided/pro specification wizard | 1–2 |
| CU-04 | `/quotes/[id]` | Quote version and alternatives, approval/negotiation | 2 |
| CU-05 | `/orders/[id]` | Tabbed workspace: spec, proof, messages, payments, timeline | 2–3 |
| CU-06 | `/orders/[id]/artwork` | Upload/review/comment and sign off proofs | 2–3 |
| CU-07 | `/payments` | Advance/balance, receipts | 3–4 |
| CU-08 | `/address-book` | Pickup and delivery addresses | 1–3 |
| CU-09 | `/reorder` | Select prior job, adjust quantity, request quote | 3–4 |
| CU-10 | `/organization` | B2B team accounts, negotiated rates | 5 |

## Planned: business dashboard
| Screen ID | Route idea | Scope | Phase |
|---|---|---|---|
| BO-01 | `/business/dashboard` | KPI overview, alert inbox, delayed jobs, receivables | 1–3 |
| BO-02 | `/business/leads` | Unified source list, assignment and follow-ups | 1 |
| BO-03 | `/business/customers` | CRM, history and contacts | 1 |
| BO-04 | `/business/quotes` | Estimator workbench, quote revisions, margin approval | 2/6 |
| BO-05 | `/business/orders` | Job workspace, approval gates, status and work split | 2–3 |
| BO-06 | `/business/production/board` | Kanban by press/finishing stage, schedule | 3/5 |
| BO-07 | `/business/inventory` | Materials, stock levels, reservation and procurement | 3/5 |
| BO-08 | `/business/vendors` | Vendors, scoped work packages, handoffs | 5 |
| BO-09 | `/business/inbox` | WhatsApp, email, supported social messages | 4 |
| BO-10 | `/business/accounting` | Invoices, receipts, credit, reports | 3–4 |
| BO-11 | `/business/deliveries` | Pickup, dispatch, POD, split shipment | 3–5 |
| BO-12 | `/business/analytics` | Profitability, machine yield, customer ROI | 6–7 |
| BO-13 | `/business/settings` | Roles, teams, products, price book, integrations | 1–6 |

## Planned: shop-floor and external partner
| Screen ID | Route idea | Purpose | Phase |
|---|---|---|---|
| OP-01 | `/operator/jobs` | Assigned queue, job-ticket summary, QR access | 3/5 |
| OP-02 | `/operator/jobs/[id]` | Start, pause, complete, report waste and downtime | 3/5 |
| VE-01 | `/vendor/invitations` | Accept/reject scoped subcontract offer | 5 |
| VE-02 | `/vendor/jobs/[id]` | Files, specs, deadline and vendor updates | 5 |

## UI rules
1. Separate real auth boundary for customer/owner/vendor. The Phase 0 demo page has been replaced with staff sessions; later customer/vendor roles remain planned.
2. Internal cost, markup and supplier quotes are not returned to customer-facing views.
3. Prove empty state, error state, loading, permission denial, and mobile view for every screen.
4. Workspaces share order/item context, artwork version and financial snapshots, not separate disconnected copies.
5. Interactive print estimates clearly say provisional vs owner-confirmed.

## Owner/account screens in `/business`

Password-only OWNER login displays the mandatory MFA gate, without rendering staff administration or the lead queue. First setup shows the private authenticator key and requires confirmation; recovery codes display once with an explicit saved acknowledgement. Subsequent sessions use authenticator/recovery challenge. New/reset accounts see a forced-password form and re-login afterward. Unsupported operational roles see an account-ready message instead of unauthorized enquiry data.

Verified owners see staff creation, role/enable/disable controls, password resets, security activity and enquiry assignment controls. Administration mutations need verification within five minutes; “Verify owner access again” opens a fresh challenge. Sign-out clears private component state. The production owner recovery/provisioning flow remains an operator runbook task.

## Phase 1 development candidate

- `/customer`: signup, verification, login, recovery/reset, owned enquiry list/details, guest linking, profile/address/organization and privacy requests.
- `/products` and `/products/[code]`: current catalogue and product-specific request entry.
- `/request-quote`: guided/professional options from templates, privacy acknowledgement and conditional guest verification; signed-in customer identity is prefilled.
- `/privacy`: versioned development notice; production content needs owner approval.
- `/business`: ENQUIRIES, CONTACTS and owner-only STAFF, PRODUCTS, PRIVACY, LOCAL EMAIL modules. Local email preview is fictional-data-only and excluded from prod.

Manual walkthrough is recorded in `PHASE1_MANUAL_CHECKLIST.md`. Full browser automation is optional through `Test-PhaseOneBrowser.ps1`; its synthetic fixture owner does not alter the actual enrolled owner. No completed run is claimed until recorded in the Phase 1 verification report.

## Account and enquiry refinements

Business sessions use a circular account dropdown with workspace, profile/security and logout actions. Public marketing/customer login links are removed from the business header. Customer headers omit staff sign-in and retain Request a quote. Customer login/register use a centered card, full-width primary action, separate account/recovery links and collapsed verification-email help. Google/Microsoft actions show setup status until configured.

Enquiry details open in a native modal dialog with a blurred inert background, bounded internal scrolling, Escape/backdrop/close controls and focus restoration. Assignment, status and follow-up controls use consistent typography and spacing. Workspace modules have a dedicated gap above metrics; owner MFA actions have separate primary and secondary rows.

Header follow-up: logged-out visitors see Staff / owner sign in linking to /business, including on the customer page. Authenticated sessions show the circular account menu instead.

## Phase 2A.1 quotation screens
Staff enquiry popup includes quote history and a reviewed issue form for owner/assigned salesperson, with scope, INR total, optional requested date, proposed delivery/collection date, validity and terms. Customer View enquiry includes the same commercial history, specification snapshots, exact-version agreement checkbox and Request changes action. Accepted version displays Agreed delivery/collection; revised issuance is unavailable after acceptance. Feedback appears within the quotation section.

## Phase 2B.1 design review
Staff enquiry popup now includes Design files & proof review: restricted uploads/status, clean downloads, retry scan, owner designer selection, revision publication, revision history and customer-visible team comments. Customer enquiry details include reference upload, published proofs, current approval status, page/side comments and explicit exact-revision approve/request-changes controls. Draft designs are hidden. DESIGNER sign-in opens assigned design jobs with scoped proof workspace and no unrelated enquiries/commercial amounts. No inline proof rendering is claimed: customers download the exact file to review it.

## Customer live-work and account layout
/customer header no longer repeats Request a quote; the dashboard retains its primary request action. Avatar chevron is removed. Default avatar is a generated face and can be replaced/reset in profile. Your profile opens a modal with Profile, Sign-in and Privacy tabs; these forms are removed from the dashboard.

Accepted work appears in Live works; /customer/work/{id} groups stage cards, public designer/team contact, proof/image-pin review, portal conversation, email connection status, public work log and quotation agreement. Closed accepted work remains accessible through View closed work. Closed enquiries sort last and show the team's explanation.

Staff enquiry popup shows a read-only status plus Start reviewing for NEW. Prepare/send quotation includes requested-date prefill and optional phone/email acceptance with evidence, no validity field. Quote actions refresh status/revision. Reject or remove requires a reviewed customer-visible reason and archives history.
# Phase 2 completion screens — 24 October 2026

Staff enquiry modal: restore/save partial quotation draft; optional itemized pricing and owner-configured print vocabulary; review/send/download numbered PDF; reasoned withdrawal; accepted-version amendment form; explicit commercial order conversion with copied lines and clean artwork linkage. Active agreements/offers render before closed quotation history. All-inclusive legacy pricing represents a commercial lot, not a single printed copy.

Owner Products: product templates plus controlled print vocabulary category editor with expected revision and immutable configuration audit.

Designer/owner/sales design workspace: task list, due dates, internal notes and task status; immutable feedback resolution explanation; clean-file proof publication; side-by-side raster revision comparison and authenticated PDF review downloads. Customer work: current quote-bound approval/rejection/changes, resolved comment history and commercial order view. An accepted amendment requires a fresh proof linked to that agreement, even if the artwork bytes are reused.

Owner/assigned sales: collapsed phone/email proof agreement form requires a scanned shared evidence file, source, meaningful record and exact proof acknowledgement. Public approval history identifies staff-recorded offline agreement. Designer cannot use this authority. No manufacturing release button is added in Phase 2.


## 24 October staged workspace correction

Owner/designer intake shows requirements, reviewer assignment and optional accept/reject comment only. After acceptance, the popup shows quotation; after agreement acceptance, artwork/customer review; after current proof approval, a production-handoff summary. Prior agreements, responsibility, reminders, conversation and internal logs are expandable instead of simultaneous forms. Closed enquiries show their explanation and collapsed records. The visible stage is derived from authoritative agreement/proof state; server permissions still govern actions.

Sales sees assigned reviewed enquiries. Designer sees incoming intake plus assigned design work. Printer (OPERATOR), Delivery and other specialist roles see only assigned generic tasks. Contacts appears only with owner-granted access. Owner Staff administration grants/revokes it; reauthentication is required after permission changes. Owner queue Delete archives unaccepted work with the standard explanation and truthful notification feedback. Customer live work keeps artwork central and collapses agreements/logs. Full future fulfilment: FULFILMENT_WORKFLOW.md.

24 October quote review correction: request form has optional artwork attachment with separate saved-enquiry/upload feedback and immutable +91 prefix; number field accepts ten digits. Quotation paper/GSM and finished size inputs offer product-template suggestions and custom entry. Remove duplicate free-text finish/binding; controlled finishing/binding choices remain optional. Issued offer says In review—waiting for customer acceptance. Hide revised-quote form while any offer awaits response; withdrawn/change-requested versions become collapsed history. Artwork request is optional at issuance and available while awaiting response. Owner queue supports row selection and Delete selected; Delete archives accepted work with a cancellation explanation. Privacy requests are under collapsed Administration.

24 October lifecycle-mail continuation: V13 adds one-time customer setup with quotation, lifecycle customer/team notifications, owner Email activity, proof-gated manually confirmed production/QC/dispatch/delivery milestones, delivery issue holds and durable 48-hour completion. This supersedes earlier “milestone emails/manual operations/timer not implemented” notes only for this bounded slice. Photos/GPS, stock/payment production controls, inbound Gmail sync and multi-page artwork viewer remain pending. Scope, APIs and verification: WORKFLOW_EMAIL_VERIFICATION.md.

24 October signup correction: V14 repairs repeated unverified signup/resend, binds the latest chosen password to verification, and creates guest access at intake acceptance. Customer-only email carries a 24-hour temporary password with forced activation/change and privacy consent; existing passwords are retained. New mail is allowed to any real address by explicit owner request; held/synthetic mail is not replayed. This supersedes earlier recipient-only/link-only account policy. See CUSTOMER_SIGNUP_CORRECTION.md.

24 October quotation email enhancement: V15 binds outbox QUOTE messages to the immutable issued quotation. Native dispatch verifies its PDF hash and attaches that exact version; branded escaped HTML/plain alternatives include the customer workspace link, sender name and short product/reference subjects with existing thread headers. No API route or approval gate changes. Details and focused evidence: QUOTATION_EMAILS.md.

24 October PDF review slice: native resource-limited PDF inspection and authenticated local-worker multipage canvas preview, per-page location comments, full-document page acknowledgements tied to immutable revision/hash/quote, reference/file Preview dialogs and a persistent least-privilege local test designer are implemented. This supersedes earlier multipage PDF viewer pending notes only for PDF/image preview/comment review; source editing, advanced preflight, gallery reuse, delivery photos/GPS and payment/WhatsApp integration remain separate. API, migration, role and focused evidence: PDF_ARTWORK_REVIEW.md. Owner-proposed entry/payment/notification paths: ORDER_ENTRY_AND_PAYMENT_FLOWS.md.

## Review refinements (24 October)

Request quote includes nontechnical brief guidance, explicit saved/upload-in-progress feedback, and a recoverable attachment failure message. Business queue adds Status filter. Quotation preparation supports semicolon-separated custom commercial terms plus optional multi-select common terms; additional line item control is labelled “Add additional quotation item”. Issued quotations offer authenticated multipage PDF Preview beside Download.

## Customer reference and work screens
Customer enquiry: friendly Status and Stage columns; earlier-enquiry link modal; references visible without premature proof controls. Artwork-request phase: fictional category catalog, scanned selection/upload, page/image pins, batch review request and annotated-copy preview. Accepted work: five ordered stages, wide desktop work area, conversation/outgoing email sidebar, responsive stacking and aligned history/agreement panels. See CUSTOMER_REVIEW_WORKSPACE.md.


## PF-320 payments and invoices — 24 October 2026
Owner-configured advance/credit rules, immutable verified receipts/corrections, commercial invoice PDF preview/email and server-side production/dispatch gates replace the earlier Not recorded/payment-unimplemented placeholder. Customer and assigned sales are read-only; owner confirms manual finances. Optional Razorpay TEST checkout uses server-owned amounts, HMAC plus captured-payment API verification and deduplicated callbacks/webhooks. Live gateway payments and GST tax invoicing are not enabled. Routes, schema, screen behavior, security boundaries and focused verification: PAYMENTS_AND_INVOICES.md.


24 October visual experience refresh: public hero/catalogue/product-finish interactions, live quote brief, scoped customer/owner overview cards, role navigation and work/payment visual hierarchy are implemented without replacing business state. See UI_EXPERIENCE_REDESIGN.md; screenshot/test evidence is packaged separately.

## 24 October implementation-first UI completion

Reference-inspired UI covers interactive public print previews, searchable enabled products, scoped customer/business/designer summaries, stage-aware artwork samples and comments, wide work/conversation layout and phone navigation. Final polish includes sample/designer search, stage links and conversation send/refresh states. See IMPLEMENTATION_REVIEW.md for scope and manual review. One final frontend build/type check; prior focused Gradle results reused. Real Razorpay UI capture, captured external webhooks and invoice inbox delivery remain unverified; broad retries stopped at the owner’s request.

## 24 October operations continuation

Owner-authorized automatic designer routing, project-level production queues and private delivery photo evidence are implemented. PF-300/PF-330 now include printing/finishing/QC/rework/hold progression, scoped printer assignment and pickup/transit/delivery evidence. Existing proof, quotation, finance and dispatch gates remain. See OPERATIONS_WORKFLOW.md for APIs, migrations V24/V25, role scopes, upload limits, notifications and the focused Gradle/browser commands. Full shop-floor routing, inventory, capacity reservations, GPS and partial dispatch remain separate.


## 25 October workflow redesign — Batch 1

Customer work: reference-aware next action + five stages + sticky right-hand portal/email rail. Current proof: Open proof studio → desktop page rail/canvas/review controls, stacked on phone. Business: module-specific headers and scoped action inbox. Production: desktop lanes / phone lane selector → job drawer. Delivery: separate dispatch and delivery-confirmation evidence readiness, explicit missing/scan states, full original preview. See [implementation map and evidence](WORKFLOW_REDESIGN_BATCH1.md).

## 25 October workflow redesign — Batch 2

Owner enquiry screen adds live action counts for quotes, customer decisions, designer assignment, release, production exceptions and dispatch blockers, with direct links into filtered queues. Production queue filters cover release, unassigned, hold, QC/rework and dispatch blockers; desktop lanes fit the available width, while mobile keeps a single-column board and stage selector. Job drawer shows reason-based release/dispatch checks and existing role-gated production controls. Screenshot gallery and view significance: [Batch 2 evidence](WORKFLOW_REDESIGN_BATCH2.md).

Batch 2 final correction: owner production filters and dashboard counts share the QC/rework predicate; historical rework remains an audit badge. See [final verification and visual evidence](WORKFLOW_REDESIGN_BATCH2_FINAL.md).
