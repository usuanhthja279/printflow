# ADR-002 — Demo routes are never production staff authentication
**Status:** Accepted for Phase 0 · **Date:** 2026-10-23

### Context
Phase 0 needs a visible staff workflow but does not yet contain identity, password policy, multi-role authorization, consent or fraud protection.

### Decision
Read/update demo endpoints require an environment-supplied key **and are excluded when Spring `prod` profile is active**. Public enquiry write is disabled unless explicitly enabled. The Phase 0 demo may be run only on loopback/private test networks with fictional customer data. Token-based read is demonstration status access, not a customer login. Phase 1 must replace the demo endpoints with authenticated RBAC routes before commercial deployment.

### Consequences
Usable local vertical slice without pretending production security is complete. No public-facing live release of Phase 0.
