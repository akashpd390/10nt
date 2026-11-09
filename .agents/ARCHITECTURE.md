# .agents Memory & Project Rules

This folder maintains persistent agent instructions, architecture context, and implementation standards for the **10nts** project.

## Project Structure
- `backend/`: Node.js + Express + TypeScript + Drizzle ORM + Neon DB.
- `android/`: Native Jetpack Compose + Kotlin + Koin + Retrofit.

## Core Rules & Principles
1. **Balance & Ledger Logic:** Use a signed running balance where positive numbers represent advance/credit and negative numbers represent arrears/dues.
2. **Cold Start Resiliency:** Support Render free tier spindown with custom health-check retry and Android background notification.
3. **SMS Management:** Local-first SMS using Android intents/SIM.
4. **Security:** JWT (short-lived access + 7-day refresh token rotation), Argon2id/bcrypt passwords, encrypted shared preferences in Android.
