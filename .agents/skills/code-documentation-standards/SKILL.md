---
name: code-documentation-standards
description: Enforce comprehensive file headers, KDoc/TSDoc comments, and explicit documentation for every class, method, param, and state flag.
---

# Code Documentation Standards Skill

This skill enforces high-standard documentation on all created and modified source files across the repository.

## 1. File-Level Headers
Every source file (TypeScript and Kotlin) must start with a concise header explaining:
- The role of the file within the system architecture.
- Key relationships, inputs, and outputs.

### TypeScript Header Example:
```typescript
/**
 * @file payment.service.ts
 * @module modules/payments
 * @description Coordinates manual tenant payments, applies credit towards pending bills,
 * calculates new signed running balance, and writes immutable records to the ledger.
 */
```

### Kotlin Header Example:
```kotlin
/**
 * @file PaymentViewModel.kt
 * @package com.tenants.app.presentation.payments
 * @description Manages payment form input state, tenant search filtering, and invokes
 * payment submission while triggering SMS intent dispatch events.
 */
```

## 2. Classes, Interfaces & Data Types
- Document every class, interface, and DTO with a clear explanation of its purpose.
- Explain the business meaning of enum values and custom type unions.

## 3. Methods & Functions
Every public or non-trivial internal function must specify:
- What the function achieves.
- `@param` descriptions and expectations (e.g. constraints, units).
- `@return` explanation.
- `@throws` any expected exceptions or error responses.

## 4. State Flags & Configuration Constants
- Boolean state flags (e.g., `isServerColdStarting`, `isAdvanceAdjusted`, `hasPendingArrears`) must include comments clarifying what condition turns them `true` vs `false` and their downstream effects on the UI or flow.
