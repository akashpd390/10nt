---
name: backend-layered-testing
description: Standardize backend feature development with layered architecture (controller, service, repository, utility), strict input validation, and comprehensive automated test coverage for every endpoint.
---

# Backend Layered Architecture & Testing Skill

This skill enforces clean architectural boundaries and test-driven rigor on all backend endpoints.

## 1. Layered Separation of Concerns
Every module under `backend/src/modules/<feature>` must strictly separate:
- **Routes / Controller (`*.controller.ts`, `*.routes.ts`)**: HTTP request handling, DTO validation parsing (Zod/Joi), HTTP status codes, and delegating to services. Never place database queries or business algorithms inside controllers.
- **Service (`*.service.ts`)**: Business logic, financial ledger adjustments, transaction coordination, and error dispatching.
- **Repository / DB Access (`*.repository.ts` or Drizzle queries)**: Clean database operations and queries via Drizzle ORM.
- **Utility / Helpers (`*.util.ts`)**: Pure functions, mathematical roundings, string formatters, and date utilities.

## 2. Request Validation Rules
- All incoming payloads (`req.body`, `req.params`, `req.query`) must be strictly validated before touching any service.
- Return explicit `400 Bad Request` responses with structured field-level validation errors.

## 3. Mandatory Endpoint Testing Checklist
For **every** endpoint created:
1. **Happy Path**: Successful request returning expected status code (`200 OK`, `201 Created`).
2. **Validation Failure Cases**: Missing fields, negative amounts, invalid emails, empty strings.
3. **Authorization & Boundary Cases**: Missing/invalid bearer tokens, unauthorized access to resources.
4. **Idempotency & Conflicts**: Attempting duplicate room numbers, duplicate active tenant assignments.
5. All test suites must be executable and passing via the test runner (e.g., Jest/Vitest).
