# Development Guide

## Prerequisites

- JDK 21
- Docker Desktop with Linux containers
- PowerShell 7 on Windows

## Environment

Copy `.env.example` to `.env`, load its values into the current shell, and run `docker compose up -d --wait`. The application recognizes only `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD`; defaults are suitable only for the isolated demo.

## Commands

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd javafx:run
.\mvnw.cmd -Pscreenshots javafx:run
.\database\reset.ps1
```

Set `APP_SMOKE_TEST=true` to make a successful application startup close automatically after three seconds. Integration tests create their own MySQL 8.4 container.

## Package layout

Use `domain` for vocabulary, `service` for use cases, `repository` for ports, `repository.jdbc` for MySQL adapters, `config` for composition, and `ui` for JavaFX. UI code must not issue SQL. Money must remain `BigDecimal` with explicit scale and rounding.

## Release package

Run `scripts/package-release.ps1` from the repository root. It verifies the build, then creates a portable ZIP containing the application JAR, runtime dependencies, launch scripts, documentation, and a SHA-256 manifest. The package requires JDK 21 and a configured MySQL database; it is not an installer.
