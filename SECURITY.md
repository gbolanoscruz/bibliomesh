# Security Policy

## Scope & Supported Versions

`bibliomesh` is an open-source school library management platform. Security updates and patches are applied to the latest state of the `main` branch.

| Branch / Version | Supported |
| :--- | :--- |
| `main` (latest) | :white_check_mark: |
| Feature branches / older commits | :x: |

---

## Active DevSecOps Controls

- **Branch Protection (`main`):** GitHub Rulesets block direct pushes and require Pull Requests with passing status checks.
- **Vulnerability Scanning:** **Trivy** runs automatically on every Pull Request to catch `CRITICAL` and `HIGH` CVEs and misconfigurations.
- **Static Analysis (SAST):** **CodeQL** continuously analyzes Java, TypeScript/JavaScript, and GitHub Actions workflows.
- **Dependency Hygiene:** **Dependabot** monitors Maven, npm, Docker, and GitHub Actions dependencies.
- **Student Privacy by Design:** In federated interlibrary lending, patron/student PII never leaves the home library database; only institutional loan requests and catalog availability are exchanged between peer nodes.
- **Secret Hygiene:** Database URLs, API keys, and tokens are never committed to version control.

---

## Reporting a Vulnerability

If you discover a security vulnerability, **please do not open a public GitHub Issue**.

Report it privately via **GitHub Private Vulnerability Reporting**: go to the **Security** tab → **Advisories** → **Report a vulnerability**.

### What to Expect
- **Acknowledgment:** Within 72 hours.
- **Remediation:** Validated vulnerabilities will be patched on a dedicated branch and merged into `main` through the standard security-checked PR workflow.