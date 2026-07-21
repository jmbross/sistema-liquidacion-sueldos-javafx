# Security Policy

## Scope

This is a portfolio demonstration and is not supported for production payroll use. Security reports about the current default branch are welcome.

## Reporting

Do not publish credentials, personal data, or exploitable details in an issue. Use GitHub's private vulnerability reporting feature when available. Include the affected version, reproduction steps using fictitious data, impact, and a suggested mitigation. No response-time guarantee is offered.

## Current controls

- Environment-only database credentials and a sanitized `.env.example`.
- BCrypt password hashes; authentication never compares plaintext in SQL.
- Parameterized JDBC queries and centralized repository adapters.
- Role checks in application services, not only in the UI.
- Generic user-facing errors with internal logging.
- CodeQL, dependency review, Dependabot, wrapper validation, secret scanning, SpotBugs, and tests.

## Production recommendations

A real deployment would require a managed identity system, secrets manager, TLS, least-privilege database accounts, immutable audit records, encrypted backups, log redaction, retention controls, dependency patch SLAs, signed artifacts, disaster recovery, and an independent review of legal payroll rules. The demo credentials and seed data must never be promoted.

## Demo data policy

All versioned records are visibly fictitious and use reserved local domains or `DEMO-` identifiers. Real personal, employment, or payroll data is prohibited in commits, tests, screenshots, issues, and build artifacts.
