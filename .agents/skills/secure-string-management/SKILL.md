---
name: secure-string-management
description: Enforce zero hardcoded raw strings, credentials, or API base URLs in code. Mandate Android string resources and BuildConfig / backend environment variables.
---

# Secure String & Endpoint Management Skill

This skill forbids hardcoded strings, plain-text URLs, and credentials across both Android and Backend projects.

## 1. Android String & URL Rules
- **UI Strings**: Never write raw user-facing text inside Composables (e.g., `Text("Pay Rent")`). All user-facing text, error messages, and dialog labels must be defined in `res/values/strings.xml` and referenced via `stringResource(R.string.pay_rent)`.
- **API Base URLs**: Never hardcode base URLs or IP addresses inside Retrofit builders or Kotlin files.
  - Define URLs inside `build.gradle.kts` via `buildConfigField`:
    ```kotlin
    buildConfigField("String", "BASE_URL", "\"${project.findProperty("BASE_URL") ?: "https://api.yourdomain.com/"}\"")
    ```
  - Reference via `BuildConfig.BASE_URL`.
- **Secrets & Keys**: Sensitive tokens, salts, or crypto keys must never be committed in plain source code. Use Gradle properties, local `.env`, or Android `EncryptedSharedPreferences`.

## 2. Backend Environment Rules
- All ports, database connection strings, JWT secrets, and Cloudinary keys must be loaded strictly from `process.env` through a validated configuration module (e.g. `src/config/env.ts` validated with Zod/envalid).
- Fallbacks in production must fail loudly if required secrets are absent.
