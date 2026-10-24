# Printing company discovery questionnaire and decision log

**Owner sign-off required before Phase 1.** These items are not inferred facts about the company. Record answers after interviewing the owner, estimator, production supervisor, and accounts staff. Mark each as `Confirmed`, `Assumption`, or `Open` with date and responsible reviewer.

## A. Business identity and scope

### Owner answers recorded 23 October 2026

- **Confirmed, D-001 (display name only):** J.D.Printex. Legal entity name, GST registration, address and branches remain open.
- **Confirmed, D-006:** shaadi/wedding cards, hotel printable items, pamphlets, leaflets and offset gloss printing. Revenue ranking, specific hotel items and machine capabilities remain open.
- **Confirmed, business objective:** grow the business; no single operational problem has been identified as the priority.
- **Implementation assumption:** lead with service discovery and quote enquiries, then build staff follow-up and repeat-order workflows. No performance or revenue improvement is claimed.
- **Still open:** approved contact details, service area, portfolio permissions, typical quantities, quotation rules, hosting budget and business workflow sign-off. These answers do not constitute Phase 1 or launch approval.

| ID | Question | Default/open assumption | Status |
|---|---|---|---|
| D-001 | Legal/company name, GST registration, billing address, branches? | Single company and one branch | Open |
| D-002 | Primary service location and delivery radius? | Local pickup + local deliveries | Open |
| D-003 | Main customer types and estimated monthly orders? | Walk-in, small retailers, institutions, B2B | Open |
| D-004 | Number of employees and responsibilities by department? | Roles in SRS are provisional | Open |
| D-005 | Languages supported for customer communications? | English first; optional Bengali/Hindi | Open |
| D-006 | Print products by revenue and frequency (top 10)? | Brochure/card/book/packaging/custom are examples | Open |
| D-007 | Existing website, social profiles, business phone, email and domain ownership? | Unknown | Open |

## B. Order and quotation process

| ID | Question | Decision needed |
|---|---|---|
| D-008 | How do enquiries currently arrive? Share % from phone, WhatsApp, email, walk-ins, social and website. | Channel priority |
| D-009 | Who prepares quotes? Are prices negotiated or customer-specific? | Approval rules and margins |
| D-010 | Can customers order with no uploaded artwork? Who designs? | Artwork states and design charges |
| D-011 | What minimum information is needed to issue a quote? | Product wizard validations |
| D-012 | How do customer corrections/approvals arrive and get confirmed? | Proof and quote sign-off UX |
| D-013 | Do customers regularly reorder prior print files? | Reorder priority |
| D-014 | Is advance mandatory? What %, credit limits and late-balance policy? | Release gate |
| D-015 | What are the usual quotation turnaround and promised production lead times? | SLA targets |

## C. Offset machinery, stock and finishing

| ID | Question | Decision needed |
|---|---|---|
| D-016 | Exact press makes/models, max/min sheets, colours, grips/margins, setup times, average speed? | Machine registry and estimation |
| D-017 | CTP/plates in-house or outsourced? Approximate plate cost and spoilage? | Prepress routing |
| D-018 | Paper grades, GSM, parent sheet sizes, grain, stock units, suppliers and price volatility? | Materials catalogue |
| D-019 | Which operations are done in-house (lamination, cutting, folding, binding, die cutting, varnish)? | Job templates |
| D-020 | What operations are routinely outsourced and to which suppliers? | Vendor portal MVP |
| D-021 | How are wastage, rejected copies and machine downtime currently tracked? | Production analytics |
| D-022 | Are multi-part jobs, reprints, split dispatches or late design revisions common? | Domain edge cases |
| D-023 | Who quality-checks, approves rework and releases dispatch? | Permission matrix |

## D. Accounting and dispatch

| ID | Question | Decision needed |
|---|---|---|
| D-024 | GST, HSN/SAC, invoice series, tax invoice/estimate conventions (ask accountant)? | Tax configuration |
| D-025 | Current accounting tool: register, Excel, Tally or other? | Import/export strategy |
| D-026 | Cash/UPI/bank frequency; preferred payment gateway? | Payment roadmap |
| D-027 | Credit days, unpaid balance reminders and bad debt handling? | Receivables design |
| D-028 | Own delivery riders, courier, local pickup or mixed? | Fulfillment workflows |
| D-029 | Need dispatch challans, transport documents, POD signature/OTP? | Delivery schema |

## E. Integration readiness

| ID | Question | Decision needed |
|---|---|---|
| D-030 | Company WhatsApp Business account and admin access? Is API onboarding acceptable? | Official connector feasibility |
| D-031 | Meta Page/Instagram professional account, business manager and lead ads in use? | API scopes and lead capture |
| D-032 | Gmail, Google Workspace, Microsoft 365 or standalone SMTP? | Email connector choice |
| D-033 | Dedicated business telephone/IVR/cloud telephony provider? | Call logging options |
| D-034 | Consent and privacy expectations for customer proof publication in social portfolio? | Marketing and permissions |
| D-035 | Business domain, hosting budget, backup owner, preferred cloud provider and uptime needs? | Production infrastructure |

## F. Business acceptance scenarios

Prepare five realistic *synthetic* scenarios with owner: business cards, high-volume flyers, multi-page booklet, custom packaging, and an outsourced finishing operation. Capture quantity, dimensions, paper grade, all steps, cost inputs, expected turnaround, invoice terms, and approval process. The estimator must approve each modeled workflow before automated pricing is launched.

## Decision record template

```text
Decision ID:
Owner / approver:
Date:
Question / problem:
Options considered:
Chosen rule:
Reason:
Impacted requirements (FR/NFR):
Impacted screens / API / schema:
Revisit date / condition:
```

## Initial open design decisions

- D-A: Single printing company now; optional multi-tenant SaaS deferred.
- D-B: Customer/mobile first as responsive PWA, not native apps initially.
- D-C: Java/Next/PostgreSQL modular monolith and secure file store planned.
- D-D: Initial quotes manually reviewed; no fabricated auto-pricing until actual costing samples.
- D-E: Official WhatsApp/Meta provider capabilities and account permissions must be checked when credentials are available.
- D-F: Real production launch blocked until authentication, anti-abuse, privacy/consent, TLS, observability and tested backups exist.
