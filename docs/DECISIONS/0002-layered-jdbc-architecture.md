# ADR 0002: Layered services with JDBC adapters

- Status: Accepted
- Date: 2026-07-21

## Decision

Use immutable domain records, application services, repository interfaces, and explicit JDBC adapters. Keep JavaFX as a presentation adapter and compose dependencies in `ApplicationBootstrap`.

## Rationale

The design makes business and authorization behavior independently testable while retaining transparent SQL appropriate to the size of the case study. An ORM would add abstraction without demonstrating a necessary benefit here.
