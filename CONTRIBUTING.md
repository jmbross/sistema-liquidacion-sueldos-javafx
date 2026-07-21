# Contributing

This repository is maintained as a focused portfolio case study. Before proposing a change, open an issue that states the observed problem, expected behavior, security impact, and intended validation.

## Development standard

1. Use JDK 21 and a feature branch.
2. Never commit secrets, personal data, generated binaries, database dumps, or IDE metadata.
3. Keep UI, application, domain, and persistence responsibilities separated.
4. Add or update tests with every behavior change.
5. Run `./mvnw.cmd clean verify` on Windows or `./mvnw clean verify` on Unix.
6. Use conventional commits and explain tradeoffs in the pull request.

Security reports must follow [SECURITY.md](SECURITY.md), not public issues.
