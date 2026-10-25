# WhatsApp Cloud API setup

## Readiness

- **IMPLEMENTED:** the official Graph API text adapter, signed webhook handling, deterministic bot, encrypted message storage, local simulator and owner/sales inbox.
- **TESTED LOCALLY:** focused webhook-signature/outbound-policy/settings unit tests and nine native PostgreSQL `StaffScopeIntegrationTest` cases passed; the UI flow was verified with the local simulator and synthetic browser fixtures. Exact commands/results are in `WORKFLOW_REDESIGN_BATCH5.md`.
- **PROVIDER-CONFIGURED:** no. Keep `WHATSAPP_ENABLED=false` until the business assets, permissions and webhook are configured.
- **LIVE-VERIFIED:** no live Meta message has been sent or received as part of this change.

## Required business assets

Create or select a Meta app, a WhatsApp Business Account, and a verified business phone number eligible for the Cloud API. Configure the app's WhatsApp product and subscribe the app to the `messages` webhook field. A public HTTPS callback is required for provider delivery; local loopback is not a public callback. Use an approved tunnel or deployment callback only after review. Do not use a personal WhatsApp account, WhatsApp Web, browser automation or unofficial gateways.

Set server-side environment variables only:

```text
WHATSAPP_ENABLED=false
WHATSAPP_GRAPH_API_VERSION=
WHATSAPP_PHONE_NUMBER_ID=
WHATSAPP_BUSINESS_ACCOUNT_ID=
WHATSAPP_ACCESS_TOKEN=
WHATSAPP_APP_SECRET=
WHATSAPP_VERIFY_TOKEN=
APP_WHATSAPP_ENCRYPTION_KEY=
```

Generate `APP_WHATSAPP_ENCRYPTION_KEY` as Base64 for 32 random bytes and back it up securely. It encrypts phone numbers, contact names and conversation text. Losing it makes existing stored WhatsApp content unreadable. Keep Meta credentials and this key out of source control, logs, browser variables and support screenshots. For local simulation use `APP_WHATSAPP_SIMULATOR_ENABLED=true`; it requires an authenticated owner with MFA and is disabled in production. No provider call is made by the simulator.

Public click-to-chat is independently controlled by `WHATSAPP_PUBLIC_CONTACT_ENABLED=true` and `WHATSAPP_PUBLIC_NUMBER`. This exposes only the configured business number and generic prefilled text. It does not enable sending through the API.

The webhook routes are `GET` and `POST /api/public/whatsapp/webhook`. Configure the Meta callback as `https://<approved-host>/api/public/whatsapp/webhook`, use the exact `WHATSAPP_VERIFY_TOKEN`, and subscribe to `messages`. The app-secret HMAC is validated over the raw body. Never put the verify token or app secret in a URL.

## Local simulator

1. Leave `WHATSAPP_ENABLED=false` and set `APP_WHATSAPP_SIMULATOR_ENABLED=true` in the ignored local `.env`.
2. Start the native application and sign in as an MFA-verified owner.
3. Open Business → WhatsApp inbox → Local simulator; send `MENU`, then reply with menu numbers. Enter a synthetic international phone number only.
4. Test “Talk to our team,” claim the handoff as sales, send an in-window response, and resume the bot.
5. Confirm outgoing messages remain `QUEUED` in simulator mode; this is not provider delivery.

The bot can outline a request and link to the secure quote wizard. It never creates a binding quote/order or discloses account, payment, artwork or order data over an unverified phone contact.

## Turning on provider delivery

Only after owner approval and provider setup, configure all required values and set `WHATSAPP_ENABLED=true`. Restart Spring once. Startup validation refuses incomplete live configuration. Verify the webhook GET challenge and a synthetic/approved test event before approving any real contact. Use Meta's test number first and only an explicitly approved recipient. Check provider status callbacks before treating a message as delivered. Keep sends disabled if webhook delivery/read status cannot be verified.

The outbox supports text in the customer-service window, limited retries for definite 429/5xx responses, and no retry after an ambiguous timeout. It does not implement approved message templates, campaigns, reminders or notifications outside the 24-hour window. Those messages remain blocked. Provider pricing and current policy should be reconfirmed in Meta's official documentation before activation.
