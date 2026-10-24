# ADR-001 — Modular monolith first
**Status:** Accepted for Phase 0 · **Date:** 2026-10-23

### Context
Single small-to-medium printing company, limited team, interdependent order/stock/finance transactions and rapid Codex-assisted delivery. Microservices would add message contracts, operations burden and distributed transactions before justified by scale.

### Decision
Java 21 + Spring Boot modular monolith with package boundaries and PostgreSQL. Next.js as separate web process; PDF workers and connectors can later run out of process. Shared outbox for asynchronous notifications.

### Consequences
Faster iteration, easy data transactions, simpler debug/deploy. Business boundaries must still be explicit. Splitting into services later remains possible but not an MVP goal.
