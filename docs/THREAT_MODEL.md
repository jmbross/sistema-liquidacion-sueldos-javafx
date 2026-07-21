# Threat Model

## Assets and trust boundaries

Assets include password hashes, user/worker records, rates, receipts, database credentials, and generated PDFs. Trust boundaries exist between the operator and JavaFX UI, the application and MySQL, the repository and CI, and generated artifacts and their recipients.

## Primary threats and controls

| Threat | Current control | Residual risk |
|---|---|---|
| Credential disclosure | environment variables, ignored `.env`, masked logging | host process and local files remain operator-controlled |
| Password theft | BCrypt hashes and generic login errors | no MFA, rate limiting, or managed identity |
| SQL injection | prepared statements and validated input | future queries require the same discipline |
| Authorization bypass | service-level role checks and assignment filtering | simplified two-role model |
| Record tampering | DB constraints and receipt transaction | no append-only audit trail or signatures |
| Sensitive data in Git | fictitious data policy and secret scanning | scanners cannot prove absence of every contextual identifier |
| Vulnerable dependency | Dependabot, dependency review, CodeQL, SpotBugs | patching still requires maintainer action |
| Malicious artifact | CI build and SHA-256 checksums | artifacts are not cryptographically signed |

## Non-goals

The repository does not claim compliance, legal payroll accuracy, production hardening, multi-tenancy, or internet-facing operation. Those require jurisdictional review, stronger identity, encryption, auditability, monitoring, recovery, and independent security assessment.
