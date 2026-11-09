# 10nts Codebase Index & File Catalog

This file acts as the project index. It documents every file created, its purpose, and its primary symbols so you and AI assistants never need to perform blind searches or full scans.

---

## Root Configurations & Agent Memory
- `.env.example`: Environment variables template for database, JWT, Cloudinary, and ports.
- `rough_plan.md`: Original user requirements and architecture notes.
- `.agents/ARCHITECTURE.md`: Core system architecture and engineering rules.
- `.agents/rules/implementation_acknowledgement.md`: Technical acknowledgement and blueprint.
- `.agents/skills/git-commit-workflow/SKILL.md`: Structured phase/session git commit standards.
- `.agents/skills/backend-layered-testing/SKILL.md`: Controller -> Service -> Repository separation & test coverage.
- `.agents/skills/android-core-testing/SKILL.md`: ViewModels, services, and non-UI test standards.
- `.agents/skills/compose-clean-architecture/SKILL.md`: Unidirectional data flow & event-driven UI rules.
- `.agents/skills/compose-previews/SKILL.md`: Mandatory @Preview methods with default mock states.
- `.agents/skills/codebase-index-maintenance/SKILL.md`: Registry update rules avoiding full code scans.
- `.agents/skills/secure-string-management/SKILL.md`: No raw strings or hardcoded URLs/secrets.
- `.agents/skills/code-documentation-standards/SKILL.md`: File headers, KDoc/TSDoc, method/param/flag comments.
- `.agents/skills/workspace-boundary-and-deletion-safety/SKILL.md`: Workspace boundary controls and mandatory BIG CAPITAL deletion confirmation.
- `.agents/skills/test-driven-bugfix-and-feature-dev/SKILL.md`: Test-First TDD methodology for new services and bug fixes.

---

## Backend Modules (`backend/`)
- `backend/package.json`: Dependencies and scripts (Drizzle, Express, Neon, Vitest, TypeScript).
- `backend/tsconfig.json`: TypeScript compiler configuration (ES2022, CommonJS, strict mode).
- `backend/drizzle.config.ts`: Drizzle ORM configuration for PostgreSQL migrations and schema mapping.
- `backend/src/config/env.ts`: Schema-validated environment config with Zod.
- `backend/src/db/schema/index.ts`: PostgreSQL table definitions for users, rooms, tenants, bills, payments, and ledger entries with signed balance support.
- `backend/src/db/index.ts`: Neon serverless database client connection.
- `backend/src/db/migrations/0000_fearless_rachel_grey.sql`: Initial generated SQL migration for full schema.
- `backend/src/middleware/auth.middleware.ts`: Bearer token validation middleware injecting authenticated user into Express request.
- `backend/src/modules/auth/auth.service.ts`: Bcryptjs password hashing and JWT token generator/verifier.
- `backend/src/modules/auth/auth.controller.ts`: Handlers for login, refresh token rotation, and password updates.
- `backend/src/modules/auth/auth.routes.ts`: Routes for `/api/auth` endpoints.
- `backend/src/modules/auth/auth.test.ts`: Unit test suite verifying hashing and token verification.
- `backend/src/modules/rooms/rooms.controller.ts`: CRUD controller for rooms with occupancy validation and deletion safeguards.
- `backend/src/modules/rooms/rooms.routes.ts`: Routes for `/api/rooms`.
- `backend/src/modules/tenants/tenants.controller.ts`: Tenant registration, room occupancy binding, and deletion safeguard if unpaid dues exist.
- `backend/src/modules/tenants/tenants.routes.ts`: Routes for `/api/tenants`.
- `backend/src/modules/billing/ledger.util.ts`: Financial ledger calculations for electricity units, total bills, and signed running balances.
- `backend/src/modules/billing/ledger.test.ts`: TDD test suite validating bill calculations, signed balance mutations, and advance adjustments.
- `backend/src/modules/payments/payments.controller.ts`: Manual payment entry handler with automatic ledger logging and mobile SMS notification template generation.
- `backend/src/modules/payments/payments.routes.ts`: Routes for `/api/payments`.
- `backend/src/app.ts`: Express application initialization, CORS, route registration, and `/health` wakeup endpoint.
- `backend/src/app.test.ts`: Sanity & health check test suite.

---

## Android Mobile Client (`android/`)
*(To be populated as files are created in Phase 4)*
