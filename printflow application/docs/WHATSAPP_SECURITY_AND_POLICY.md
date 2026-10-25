# WhatsApp security and messaging policy

## Data and identity

Webhook signatures use HMAC-SHA256 over the unmodified body and a constant-time comparison. Bodies are capped at 256 KiB, parsed without logging, and provider event IDs are unique. Conversation phone, contact name and text are encrypted using the dedicated `APP_WHATSAPP_ENCRYPTION_KEY`; the database stores a one-way phone hash for conversation lookup. Provider credentials and the encryption key remain server-only.

A phone number, display name, enquiry reference or WhatsApp message does not authenticate a customer. The assistant routes private order status to the authenticated workspace or secure tracker. It never sends credentials, private artwork, invoices, payment history, tracking tokens or private order details over WhatsApp. WhatsApp contacts are not automatically merged with customer accounts.

## Access control

Business inbox endpoints are under `/api/v1/staff/whatsapp`; existing security permits only OWNER and SALES. OWNER access retains the existing MFA gate. SALES can read assigned conversations and unassigned conversations that explicitly require human attention; a claim assigns one conversation. Other staff roles and customers are denied. A reply is restricted to the assigned salesperson or owner. The local simulator is owner-only, MFA-protected, non-production and flag-gated.

## Consent and outbound policy

Website privacy consent and optional WhatsApp-update consent are separate. An opt-in is recorded with its purpose, channel and version. Replying to an inbound message in the 24-hour customer-service window is distinct from a business-initiated update. This batch sends no proactive updates or marketing. A free-form reply after the 24-hour window is rejected because no approved template is configured. `STOP`, `UNSUBSCRIBE` and `CANCEL` immediately record withdrawal and suppress later business-initiated messages; a minimal opt-out acknowledgement may be sent in the just-opened service window. Further inbound messages remain recorded and can create a staff handoff without a bot reply.

`WHATSAPP_ENABLED` is off by default. The public click-to-chat number has a separate flag and never activates server-side sends. Live provider acceptance is recorded as `SENT`, not `DELIVERED`; signed provider callbacks advance status. Definite rate-limit or server failures have at most four attempts with bounded backoff. A timeout with unknown acceptance is `UNKNOWN` and is not retried to prevent duplicate messages. Permanent provider errors are visible in the staff timeline. No send can override opt-out or service-window policy.

## Incident and retention notes

Restrict database and backups to authorized operators, preserve the dedicated encryption key, rotate/revoke Meta tokens through the provider, and disable `WHATSAPP_ENABLED` if the webhook or delivery state is suspect. Current implementation has no automated retention purge, attachment ingestion, provider template support, data-subject export for WhatsApp records, or multi-instance outbox coordination; these are explicit operational limitations, not hidden capabilities. Complete retention/erasure and production threat-model review before launch.
