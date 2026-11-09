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
- `backend/src/modules/dashboard/dashboard.controller.ts`: Dashboard aggregation controller (room occupancy, total people count, pending vs advance finances, sorted unpaid tenants list, and last 5 transactions).
- `backend/src/modules/dashboard/dashboard.routes.ts`: Routes for `/api/dashboard`.
- `backend/src/modules/export/export.util.ts`: PDFKit and CSV generator for tenant rent roll, ledger history, and monthly invoices.
- `backend/src/modules/export/export.test.ts`: TDD unit tests for CSV stringification and PDF buffer magic byte generation.
- `backend/src/modules/export/export.controller.ts`: Endpoints for streaming PDF invoices and downloading CSV reports.
- `backend/src/modules/export/export.routes.ts`: Routes for `/api/export`.
- `backend/src/app.ts`: Express application initialization, CORS, route registration, and `/health` wakeup endpoint.
- `backend/src/app.test.ts`: Sanity & health check test suite.

---

## Android Mobile Client (`android/`)
- `android/settings.gradle.kts`: Gradle settings including `:app` module.
- `android/build.gradle.kts`: Root Gradle build file with Compose & Kotlin plugins.
- `android/gradle/libs.versions.toml`: Version catalog with Koin 4.0, Compose 2025, Retrofit 2.11, OkHttp, and Security Crypto.
- `android/app/build.gradle.kts`: App module build configuration with `buildConfigField("BASE_URL", ...)`.
- `android/app/src/main/AndroidManifest.xml`: Manifest with Internet, Foreground Service (dataSync), and commented direct SEND_SMS permission.
- `android/app/src/main/res/values/strings.xml`: User-facing strings (zero raw string rule).
- `android/app/src/main/res/values/themes.xml`: XML base theme.
- `android/app/src/main/java/com/tenants/app/presentation/theme/Color.kt`: Emerald & Parchment palette matching DESIGN.md.
- `android/app/src/main/java/com/tenants/app/presentation/theme/Type.kt`: Roboto Flex typography scales.
- `android/app/src/main/java/com/tenants/app/presentation/theme/Theme.kt`: Jetpack Compose Material 3 Theme wrapper.
- `android/app/src/main/java/com/tenants/app/data/local/TokenManager.kt`: EncryptedSharedPreferences for AES-256 JWT persistence.
- `android/app/src/main/java/com/tenants/app/data/remote/model/ApiModels.kt`: DTOs for Auth, Dashboard, Rooms, Tenants, and Payments.
- `android/app/src/main/java/com/tenants/app/data/remote/TenantsApiService.kt`: Retrofit client definition.
- `android/app/src/main/java/com/tenants/app/service/ServerWakeupForegroundService.kt`: Render sleep recovery foreground service with smart lifecycle notification suppression.
- `android/app/src/main/java/com/tenants/app/utils/SmsSender.kt`: Dual-mode SMS abstraction (Interactive Intent default vs commented Direct SIM).
- `android/app/src/main/java/com/tenants/app/di/AppModule.kt`: Koin DI module with cold-start network timeout interceptor.
- `android/app/src/main/java/com/tenants/app/TenantsApplication.kt`: Application class initializing Koin.
- `android/app/src/test/java/com/tenants/app/service/ServerWakeupServiceTest.kt`: Non-UI unit tests for lifecycle state detection and SMS formatting.
