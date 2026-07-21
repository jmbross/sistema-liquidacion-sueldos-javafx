# AI-Assisted Engineering

## Scope of assistance

AI assistance supported repository comparison, migration planning, code scaffolding, test design, documentation structure, and repetitive validation commands. Engineering decisions remained constrained by the recovered code, the stated portfolio objective, and executed evidence.

## Validation discipline

No generated behavior was accepted merely because it compiled. Each modernization concern was tied to an executable check:

| Decision or change | Validation |
|---|---|
| Monetary rules use `BigDecimal` | unit tests for deductions and rounding |
| BCrypt authentication | hashing and authentication tests |
| Role enforcement | authorization and integration tests |
| JDBC/Flyway migration | disposable MySQL 8.4 Testcontainer |
| Receipt transaction and PDF | persistence integration plus PDF parsing test |
| JavaFX composition | reproducible snapshots and a real database startup smoke run |
| Repository hygiene | history-aware Gitleaks and contextual file/data scans |
| Build reproducibility | Maven Wrapper `clean verify` locally and in CI |

## Human and automated controls

Recovered implementations were not merged by filename. Concepts were compared, insecure patterns were rejected, and the canonical project was rebuilt around explicit boundaries. Automated controls include compilation, tests, static analysis, formatting, coverage, CodeQL, dependency review, Dependabot, wrapper validation, and secret scanning. Visual output was inspected after being exported from real JavaFX scenes.

## Traceability

Architectural rationale is recorded in `docs/DECISIONS`; expected behavior lives in tests; build and security policy live in workflows; observed results are recorded in the portfolio professionalization reports. AI accelerated execution but did not replace engineering review, threat analysis, or evidence.
