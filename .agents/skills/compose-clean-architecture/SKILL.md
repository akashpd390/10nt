---
name: compose-clean-architecture
description: Enforce pure unidirectional data flow (UDF), event-driven callbacks, and zero business logic within Jetpack Compose UI composables.
---

# Jetpack Compose Clean Architecture Skill

This skill enforces strict Unidirectional Data Flow (UDF) and separation of UI rendering from business logic.

## 1. Zero Business Logic in Composables
- Composables are **dumb/declarative renderers**.
- Never perform arithmetic calculations, network calls, database queries, formatting conversions, or state mutations directly in a `@Composable` function.
- All domain decisions and mutations belong exclusively inside the `ViewModel` or dedicated domain mappers.

## 2. Unidirectional Data Flow (UDF) & Event Architecture
Every screen must follow the `State -> UI -> Event` pattern:
1. **State In**: The composable receives an immutable state data class (e.g., `PaymentUiState`).
2. **Events Out**: The composable exposes lambda callbacks for all user interactions (e.g., `onAmountChange: (String) -> Unit`, `onSubmitClick: () -> Unit`, `onTenantSelected: (Tenant) -> Unit`).
3. **Screen vs. Content Separation**:
   - Provide a top-level route composable that connects to the `ViewModel` and collects state:
     ```kotlin
     @Composable
     fun PaymentScreen(viewModel: PaymentViewModel = koinViewModel()) {
         val state by viewModel.uiState.collectAsStateWithLifecycle()
         PaymentContent(
             state = state,
             onEvent = viewModel::onEvent
         )
     }
     ```
   - Provide a stateless `*Content` composable that only takes `state` and lambda callbacks. This ensures instant reusability and previewability.
