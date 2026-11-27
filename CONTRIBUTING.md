# Contributing to BiblioMesh

Thank you for contributing to `bibliomesh`. This repository follows a **Docs as Code** and **DevSecOps-first** workflow to keep `main` clean, secure, and deployable on a $0 serverless cloud stack.

---

## 1. Branching & Pull Request Workflow

Direct pushes to `main` are blocked by GitHub Rulesets. All changes must go through a Pull Request:

1. Sync your local `main` branch:
   ```bash
   git checkout main
   git pull origin main
   ```
2. Create a descriptive branch from `main` using one of these prefixes:
   - `feat/<short-description>` — New features or modules
   - `fix/<short-description>` — Bug fixes or CVE patches
   - `docs/<short-description>` — Documentation, domain specs, or ADRs
   - `ci/<short-description>` — GitHub Actions, security workflows, or tooling
   - `chore/<short-description>` — Maintenance, refactors, or dependency updates
3. Open a Pull Request targeting `main` and keep changes focused on a single concern.

---

## 2. Commit Message Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

```text
<type>(<optional-scope>): <short imperative description>
```

**Examples:**
- `chore: add initial repo structure, docs, and Trivy security workflow`
- `feat(catalog): add ISBN lookup with Google Books and Open Library fallback`
- `feat(circulation): support USB barcode scanner and camera ISBN decoding`
- `docs(adr): add ADR-001 for Spring Modulith architecture`

---

## 3. Architectural Standards

- **Backend (`backend/`):** Modular Monolith built with **Java 25 + Spring Boot + Spring Modulith**. Modules (`catalog`, `patrons`, `circulation`, `opac`, `interlibrary`) communicate via clean internal APIs and application events, verified by Spring Modulith architectural tests.
- **Database:** **Neon Serverless PostgreSQL** with schema changes managed strictly via **Flyway** migrations (`V*__*.sql`).
- **Frontend (`frontend/`):** **Next.js (TypeScript + Tailwind CSS)** with responsive barcode scanning (USB HID + mobile camera).

---

## 4. Required Quality & Security Checks

Before merging into `main`:
- **Trivy Vulnerability Scanner** *(Required)* must pass with no blocking `CRITICAL` or `HIGH` vulnerabilities.
- **CodeQL Analysis** must pass without new security alerts.
- **Secret Hygiene:** Never commit credentials, `.env` files, or API keys.