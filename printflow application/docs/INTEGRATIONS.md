# Communication, social media, payments and file/work-sharing integrations

The native Gmail send-only adapter and lifecycle outbox are implemented for recipient-restricted local testing; see WORKFLOW_EMAIL_VERIFICATION.md. Other integration plans below remain future unless separately verified. Every provider has different terms, scopes, costs and approval requirements: verify these when implementing, not merely at design time.

## Connector architecture

Connector contract: `InboundEvent(provider, provider_event_id, external_conversation_id, external_contact, channel, type, created_at, payload_reference)` -> webhook signature verification -> deduplicated inbox -> identity resolution -> `Lead/Communication/Task` creation. Outbound communication uses a provider-neutral `OutboundMessage` tied to order/enquiry with explicit consent, intent, audience, template when required and idempotency key. Credentials must reside in secure secrets, never in frontend or source control.

### WhatsApp Business Platform
- Official Meta Cloud API or authorized BSP; one company business account/number and app review/verification as required.
- Webhook verification; incoming message, delivery/read status, template and free-form service-window policies.
- Do not scrape WhatsApp Web or rely on a personal phone number to obtain programmatic message history.
- Persist opt-in, template purpose, external message ID, error codes, rate-limit/quality concerns and provider charges.
- Only send a proof/quote link scoped to relevant customer; avoid posting sensitive attachments directly into external chats unless approved.
- Planned channels: customer support, quote ready, proof sign-off, advance reminder, production milestone, dispatch.

### Facebook / Instagram / Meta Lead Ads
- Instagram professional accounts and connected Facebook Page where required; approved scopes for supported messaging endpoints and lead forms.
- Lead-ads webhook ingestion, lead details pulled with permitted token/scopes; maintain campaign/advert identifiers for attribution.
- Messenger and Instagram DMs only where supported under actual app permissions and recipient messaging policy; cannot assume unrestricted access to ordinary social accounts.
- Message/webhook retries, token revocation and app review are first-class error cases.
- Phase 2/4: inbound leads -> CRM; Phase 7: content approval and publishing where permitted.

### Email (Gmail / Microsoft 365 / SMTP provider)
- OAuth over shared-password login when supported; use minimum required scopes.
- Store provider thread ID and immutable message IDs for dedup; process attachments through malware scan and secure file storage.
- Decide between mailbox read/sync provider watch and explicit send-only SMTP based on company needs.
- Draft vs send permissions; avoid automatically replying with an AI-generated price without staff approval.

### Phone and cloud telephony
- Basic: click-to-call and authenticated manual call logs.
- Advanced: cloud telephony (e.g. approved Indian provider), provider webhooks, missed calls, IVR/routing and recordings only where legally appropriate.
- Ordinary personal mobile calls cannot be collected automatically by the website; avoid promising this.
- Associate telephone lead by verified phone ownership or staff confirmation, not speculative identity guessing.

### Google Business Profile
- Maintain website, service and review links; use supported APIs for profile/reviews/performance after access approval.
- Do **not** design Google Business Messages live chat; it was discontinued.
- Use tracked website/phone/WhatsApp contact links instead.

### Payments (Indian gateway)
- Phase 4 payment link or checkout via authorised payment service; never retain card data.
- Signed webhook, unique provider payment ID, reconciliation and webhook replay tests.
- Invoice numbering, GST/e-invoicing thresholds, HSN/SAC and credit controls confirmed with accountant.
- Advances, installments, refunds and manual bank entries use immutable ledger events.

### Work sharing
- Internal collaboration is local DB app (tasks/messages) not WhatsApp group automation.
- Partner invitation uses signed, expiring onboarding link. After login, RBAC scope to subcontract ID, work package file grants, dates and vendor purchase price only.
- File access audited and revokeable. Minimize sharing personally identifiable client information.
- Keep customer billing amounts and internal margin invisible to external suppliers.

## Integration implementation checklist
1. Provider legal/app review and exact scope feasibility documented.
2. Obtain test account, credentials and isolated webhook endpoints.
3. Define inbound/outbound schema, signature validation, idempotency.
4. Persist inbox/outbox, retry/dead-letter, delivery status and observability.
5. Apply contact-consent and channel rules.
6. Write SDK/HTTP adapter, use contract fixtures and sandbox tests.
7. Test provider unavailable, duplicate/out-of-order event, revoked token and malformed payload.
8. Activate behind per-provider feature flag after UAT and privacy review.

## Helpful official references (verify current guidance)
- https://developers.facebook.com/docs/whatsapp/cloud-api/
- https://developers.facebook.com/docs/marketing-api/guides/lead-ads/
- https://developers.facebook.com/docs/messenger-platform/instagram/
- https://developers.google.com/gmail/api/guides/push
- https://developers.google.com/my-business/
- https://razorpay.com/docs/payments/payment-links/

## Phase 1 identity email adapter

Selected provider: Google Workspace/Gmail. The SMTP adapter/outbox is implemented but defaults disabled; no configured/live delivery is claimed. Native setup generates a separate private APP_MAIL_ENCRYPTION_KEY; preserve it securely. APP_MAIL_ENABLED requires configured credentials, port 587 STARTTLS and an HTTPS APP_PUBLIC_BASE_URL. Use provider-supported application/relay credentials; never commit them or place them in frontend public configuration.

Native DEV/QA owner-only local preview is restricted to @example.test recipients and disabled when real delivery is enabled. Production excludes the preview entirely. Identity links expire after 30 minutes and contain tokens in URL fragments; do not log or share them. Production signup also requires owner-approved privacy settings. Configure/test provider delivery separately before release.

Guest production Turnstile is implemented as a server-side Siteverify adapter with hostname/action checks. Native development needs no external challenge. Real provider success and accessible fallback operations remain deployment checks. References: https://developers.cloudflare.com/turnstile/get-started/server-side-validation/ and https://support.google.com/a/answer/176600.

24 October local Gmail setup preparation: native DEV permits exact loopback HTTP only with explicit recipient restriction and a send-after activation boundary; historical and non-allowed mail is held. Production keeps HTTPS. Enable-LocalGmail.ps1 prompts privately, sends one setup test, saves settings on SMTP acceptance and restarts native services. Temporary mailbox is selected; private credential and real delivery verification remain pending. See GMAIL_LOCAL_SETUP.md.

Gmail activation update: system SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD are now supported (SPRING_MAIL_PASSWROD compatibility alias). Google SMTP accepted the setup test and local mail is enabled for the selected inbox only, with historical queue hold. Actual inbox receipt/UI flow remains user validation. Proof/production/delivery milestone notifications remain planned. GMAIL_LOCAL_SETUP.md supersedes the earlier pending-credential note.

## 24 October: owner-selected email grouping

New project emails use two phases per enquiry and recipient: INTAKE (receipt, team intake, acceptance/account invite and rejection), then WORK (first quotation, artwork, proof, production and delivery). Subjects are stable within each phase: “Enquiry received — Wedding Cards · PF-… | J.D.Printex” and “Your print order — Wedding Cards · PF-… | J.D.Printex”. The action/version stays in the message heading. Each phase starts with its own Message-ID and no reply headers; later messages reference that phase root and the preceding message for the same recipient. This prevents customer messages referencing a team-only notification. Existing LEGACY messages are not replayed or rewritten. Gmail ultimately controls visual conversation grouping. SMTP sends only; incoming mailbox replies are not imported.

## Reference review notification delivery
Reference requests bind their source/hash and generated annotated PDF to a private feedback record. Customer copies link to the customer workspace; team/designer copies link to the business workspace (explicit V21 audience flag). This remains send-only SMTP; external mailbox reply import is not represented as connected.


## PF-320 payments and invoices — 24 October 2026
Owner-configured advance/credit rules, immutable verified receipts/corrections, commercial invoice PDF preview/email and server-side production/dispatch gates replace the earlier Not recorded/payment-unimplemented placeholder. Customer and assigned sales are read-only; owner confirms manual finances. Optional Razorpay TEST checkout uses server-owned amounts, HMAC plus captured-payment API verification and deduplicated callbacks/webhooks. Live gateway payments and GST tax invoicing are not enabled. Routes, schema, screen behavior, security boundaries and focused verification: PAYMENTS_AND_INVOICES.md.
