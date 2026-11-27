## Summary

<!-- Briefly describe what this PR changes and why. -->

## Type of Change

- [ ] `feat`: New feature or module capability
- [ ] `fix`: Bug fix or CVE patch
- [ ] `docs`: Documentation, runbook, or ADR update
- [ ] `ci`: GitHub Actions, security workflows, or tooling
- [ ] `refactor`: Code structure improvement without behavior change
- [ ] `chore`: Maintenance or dependency updates

## Related Issues / ADRs

- N/A

## Testing & Verification

- [ ] Tested locally
- [ ] Verified build / automated tests pass (if applicable)

## DevSecOps & Documentation Checklist

- [ ] **Secret Hygiene:** No database credentials, API keys, `.env` files, or private keys committed.
- [ ] **Security Checks:** Trivy Vulnerability Scanner and CodeQL pass without new blocking alerts.
- [ ] **Conventional Commits:** Branch name and commit messages follow `CONTRIBUTING.md`.
- [ ] **Docs as Code:** Updated `README.md` or added an ADR in `docs/adr/` if architecture changed.