# ADR-001: Modular Monolith Architecture with Spring Boot & Spring Modulith

- **Status:** Accepted
- **Date:** 2026-11-27

## Context

BiblioMesh is designed to be deployed and operated independently by individual schools on a **$0/month serverless stack** (Google Cloud Run + Neon PostgreSQL + Vercel/Cloudflare). A school library domain involves tightly related transactional workflows—such as checking out a physical copy while updating its availability status and validating patron limits.

Splitting these bounded contexts into separate microservices would introduce network latency, distributed transaction complexity, and higher memory/operational overhead for schools hosting their own instance.

## Decision

We will build the BiblioMesh backend as a **Modular Monolith** using **Java 25**, **Spring Boot**, and **Spring Modulith** backed by a single **Neon PostgreSQL** database per school instance:

- **Explicit Module Boundaries:** Domain capabilities are isolated into top-level packages (`catalog`, `patrons`, `circulation`, `opac`, `interlibrary`).
- **Architectural Verification:** Spring Modulith tests (`ApplicationModules.of(...).verify()`) will enforce module encapsulation in CI so modules cannot bypass public APIs.
- **In-Process Events:** Cross-module side effects (e.g., updating catalog availability or audit logs on loan checkout/return) will use Spring Modulith application events rather than an external message broker.
- **ACID Transactions:** Core circulation operations execute inside single-database PostgreSQL transactions managed via Flyway migrations.

## Consequences

### Positive
- Zero distributed-transaction overhead for loans, returns, and inventory updates.
- Single Docker container to build, scan with Trivy, and deploy to Cloud Run within the free tier.
- Clean modular boundaries that preserve the option to extract a module later if ever needed.

### Negative / Trade-offs
- Slightly slower cold starts on Cloud Run compared to native-compiled microservices (mitigated with `--cpu-boost` and JVM CDS).
- Developers must respect package visibility rules enforced by Spring Modulith tests.