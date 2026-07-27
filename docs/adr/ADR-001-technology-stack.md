# ADR-001: Technology Stack

## Status

Accepted

## Context

HoldHive must be easy for a four-person training team to run locally and explain during review.

## Decision

- Backend: Java 21, Spring Boot 3, Maven, Flyway.
- Database: MySQL 8.x running locally; schema managed by Flyway.
- Frontend: React 18, TypeScript, Vite.
- Testing: JUnit 5 / MockMvc for backend, Vitest / React Testing Library for frontend.
- CI: GitHub Actions running backend, frontend, and documentation checks.

## Consequences

- The project starts with a modular monolith rather than microservices.
- Database schema changes must go through Flyway migrations.
- Frontend and backend communicate through REST contracts documented in `docs/guideline/project/api_documentation_zh.md`.
