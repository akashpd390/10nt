# 10nts System - Implementation Acknowledgement & Technical Blueprint

## 1. System Vision & Scope
A private single-landlord, multi-tenant property management system built as a clean monolith repository:
* **Backend:** Node.js, Express, TypeScript, Drizzle ORM, Neon PostgreSQL (Serverless), Cloudinary, OpenAPI/Swagger docs, Render-ready.
* **Mobile (Android):** Jetpack Compose, Kotlin, MVVM, Koin DI, Retrofit/OkHttp, Coroutines/Flow.

---

## 2. Key Decisions Confirmed

### A. SMS Strategy (Dual-Mode Design)
* **Default Mode (Active):** `ACTION_SENDTO` intent with pre-filled SMS text (`sms:<phone>?body=<message>`).
  * No dangerous `android.permission.SEND_SMS` required in `AndroidManifest.xml` by default.
  * Ensures zero runtime permission crashes upon install.
* **Direct SIM Mode (Prepared & Commented):**
  * Concrete implementation using `SmsManager.getDefault().sendTextMessage(...)` implemented in an `SmsSender` interface/implementation.
  * Clear code switches and commented permission block in `AndroidManifest.xml` so you can uncomment and enable direct background sending without UI friction whenever ready.

### B. Financial Ledger & Balance Engine
* **Signed Running Balance (`current_balance`):**
  * Negative (`< 0`): Arrears/Pending dues.
  * Zero (`0`): Fully paid.
  * Positive (`> 0`): Advance prepaid credit (auto-deducted from subsequent monthly bills).
* **Double-Entry Auditability:**
  * Every bill adds a debit entry.
  * Every manual payment adds a credit entry.
  * Ledger maintains running historical balance for CSV/PDF audit trails.

### C. Render Cold-Start Ping Service
* **Trigger:** Network request failure / 504 / Socket timeout while internet is connected.
* **Service:** Foreground service pinging `GET /health` with exponential/fixed backoff (up to ~90 seconds).
* **Lifecycle Awareness:**
  * Uses `ProcessLifecycleOwner` to detect if the app is currently in background or closed.
  * Fires a system notification only when in background.
  * If the app is focused, silently retries and updates UI without notification noise.

---

## 3. Implementation Phasing

1. **Phase 1: Backend Foundation & DB Schema**
   * Express + TypeScript setup, Drizzle ORM schema (Users, Rooms, Tenants, Bills, Payments, Ledger).
   * Migrations for Neon PostgreSQL.
2. **Phase 2: Auth & Feature Modules**
   * Auth module (JWT access + refresh token rotation, password update).
   * Room CRUD + Tenant CRUD (Cloudinary upload for Aadhaar/photo).
   * Manual Payment recording with signed balance calculations & ledger updates.
3. **Phase 3: Dashboard, Health & Export Modules**
   * Dashboard aggregates (occupancy, sorted pending dues list, last 5 transactions).
   * `GET /health` endpoint for cold-start wakeups.
   * CSV & PDF generator for tenant invoices and monthly statements.
4. **Phase 4: Android Project Setup**
   * Koin DI, Retrofit client with cold-start interceptor, EncryptedSharedPreferences.
5. **Phase 5: Mobile UI & Features**
   * Jetpack Compose UI (Dashboard, Rooms, Tenants, Payment form with tenant search dropdown).
   * SMS helper with active Interactive Intent and commented direct `SmsManager`.
   * `ServerWakeupService` with background notifications.
