# ADR-003 — Outbox and idempotent integrations
**Status:** Planned for Phase 4 · **Date:** 2026-10-23

### Context
Printing workflow generates notifications and payments whose remote providers can be unavailable. Business transactions must not depend on a WhatsApp, email or payment API being online.

### Decision
Within PostgreSQL business transaction, write business change and outbound event record atomically. A background worker dispatches, retries with capped exponential backoff, records provider message IDs and delivery state, and isolates failed messages in a dead-letter view. Receive webhooks into a deduplicated inbox verified by signatures and unique remote event IDs. Never mark a payment accepted on frontend redirects.

### Consequences
Eventual delivery, retries and observability; duplicate-safe processing becomes a mandated test suite.
