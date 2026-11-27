# ADR-002: Federated Peer-to-Peer Interlibrary Lending (ILL)

- **Status:** Accepted
- **Date:** 2026-11-27

## Context

Schools within a regional circuit often need to share books that are unavailable in their local catalog. At the same time:
1. Each school must retain **full ownership and isolation** of its own database so any institution can self-host BiblioMesh independently.
2. **Student privacy (minors' PII)** must be strictly protected—student names, IDs, grades, and borrowing histories must never be exposed to or stored in external databases.

## Decision

We will implement a **federated architecture** where each school operates its own BiblioMesh instance and Neon PostgreSQL database, and optionally registers trusted partner schools:

- **Zero Shared Databases:** No BiblioMesh node ever connects directly to another school's PostgreSQL database.
- **Signed REST Federation Contract:** Partner libraries communicate exclusively over versioned HTTPS endpoints (`/api/federation/*`) authenticated with per-partner API keys/tokens.
- **Parallel Availability Search:** Queries across partner libraries execute concurrently using Java Virtual Threads and Spring `RestClient`, returning only bibliographic metadata and copy availability counts.
- **Library-to-Library Custody:** Interlibrary loans are tracked between institutions (`REQUESTED → APPROVED → LOANED_INTERLIBRARY → RECEIVED → RETURNED_TO_ORIGIN → COMPLETED`). Student-level lending remains strictly local to the borrowing school.

## Consequences

### Positive
- Complete data sovereignty and privacy-by-design for student records.
- Any school can fork and deploy an independent $0 instance without relying on a central multi-tenant operator.
- Real-world distributed systems pattern (federation, timeouts, partial-failure resilience) layered cleanly on top of a modular monolith.

### Negative / Trade-offs
- Federated search latency depends on the slowest responding peer (mitigated with strict per-peer HTTP timeouts and graceful degradation).
- Schema/API evolution requires backwards-compatible versioning on `/api/federation/*`.