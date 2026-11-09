---
name: codebase-index-maintenance
description: Maintain and continuously update a high-level file catalog (.agents/CODEBASE_INDEX.md) documenting every file's purpose, key symbols, and relationships to prevent expensive codebase full-scans.
---

# Codebase Index Maintenance Skill

This skill maintains an up-to-date registry of all project files, their purposes, key exports, and relationships in [`.agents/CODEBASE_INDEX.md`](file:///home/koin/Projects/10nts/.agents/CODEBASE_INDEX.md).

## 1. Goal & Motivation
- Prevent blind scanning (`grep`, `rg`, or directory tree crawls) across hundreds of files.
- Give agents and developers instant lookups for specific modules, business logic, endpoints, and UI screens.

## 2. Maintenance Protocol
Whenever a file is **created**, **renamed**, or **substantially modified**:
1. Open or append to [`.agents/CODEBASE_INDEX.md`](file:///home/koin/Projects/10nts/.agents/CODEBASE_INDEX.md).
2. Record the file path, module scope, main classes/functions, and a 1-sentence description of its role.

### Format in `CODEBASE_INDEX.md`:
```markdown
### Backend: Billing Module
- `backend/src/modules/billing/billing.service.ts`: Calculates rent + per-unit electricity bills and updates signed running balances.
- `backend/src/modules/billing/billing.controller.ts`: Handles HTTP endpoints for monthly invoice generation.

### Android: Payment Feature
- `android/app/src/main/java/com/tenants/app/presentation/payments/PaymentViewModel.kt`: Manages payment entry form state, tenant selection, and ledger dispatch.
- `android/app/src/main/java/com/tenants/app/presentation/payments/PaymentScreen.kt`: Jetpack Compose UI for manual payment entry and SMS prompt.
```
