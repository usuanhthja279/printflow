# Batch 5 browser evidence

Captured from the isolated local production build on 2026-10-25 after the focused desktop/mobile Playwright run. Viewports were 1440×900 (desktop) and 390×844 (mobile), with Microsoft Edge. API responses and all identities/content are synthetic fixtures; these are not real customer records and do not verify a live Meta provider.

## Public discovery and quote request

- `public-home-{desktop,mobile}.png` — dynamic homepage and print-family finish controls.
- `public-catalogue-{desktop,mobile}.png` — catalogue listing.
- `public-product-detail-{desktop,mobile}.png` — product detail with illustrative finish selection.
- `quote-step-{1,2,3,4}-{desktop,mobile}.png` — all wizard steps, unknown-specification choices, artwork/contact and final review/consent.
- `quote-artwork-upload-retry-{desktop,mobile}.png` — saved enquiry with an attachment retry after a synthetic interruption.
- `quote-submission-success-{desktop,mobile}.png` — successful synthetic attachment scan result without duplicate enquiry creation.

## Owner WhatsApp inbox (local simulator)

- `whatsapp-inbox-{desktop,mobile}.png` — inbox list and selected workspace.
- `whatsapp-conversation-{desktop,mobile}.png` — synthetic customer conversation.
- `whatsapp-human-handoff-{desktop,mobile}.png` — owner handoff and queued reply.
- `whatsapp-opt-out-{desktop,mobile}.png` — STOP suppression and blocked reply state.

## Visual inspection

Desktop pages fit the 1440px layout with readable panels and no overlapping controls. Mobile quote and inbox layouts stack into one column; Playwright checked `scrollWidth <= innerWidth + 1` on both mobile flows. Long consent and conversation copy wraps to multiple lines. Conversation history and the local simulator extend vertically and remain reachable by scrolling. Captures are viewport screenshots; the mobile opt-out capture is taken at the active conversation state, so the beginning of the thread is above the current viewport rather than clipped from the document.

The screenshots are actual browser captures from the tested revision. They do not contain production credentials or live customer data.
