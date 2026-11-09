---
name: android-core-testing
description: Standardize unit testing for Android non-UI layers including Services, Network classes, Repositories, ViewModels, and Utilities.
---

# Android Core Testing Skill

This skill enforces strict unit and integration testing across non-UI components in the Android codebase.

## 1. Scope of Android Tests
UI layouts are tested visually via previews. This testing skill concentrates specifically on:
- **ViewModels**: StateFlow emissions, UI state mutations, user action handling, error handling, coroutine dispatching.
- **Network / API Services**: Retrofit response mapping, token interceptor refresh handling, timeout error mappings.
- **Repositories**: Local-remote sync logic, cache fallback, encrypted token persistence.
- **Background Services**: `ServerWakeupService` lifecycle, ping loops, backoff calculation, notification trigger logic.
- **Utility / Validation Classes**: Phone number formatters, currency math, input validators.

## 2. Test Architecture & Tooling
- **Unit Testing Framework**: JUnit 4 / JUnit 5 with MockK or Fake implementations.
- **Coroutines & Flow Testing**: `kotlinx.coroutines.test.runTest`, `TestDispatcher`, and Turbine for Flow assertions.
- **Architecture**: Always inject dependencies into ViewModels and Services (via constructor injection with Koin) to enable easy mocking.

## 3. Mandatory Test Coverage Rules
- Every newly created ViewModel must have a corresponding `*ViewModelTest.kt` verifying:
  1. Initial state verification.
  2. Success state transitions.
  3. Failure/error states and user message triggers.
- Network interceptors and cold-start detection logic must have test cases simulating HTTP 504 and timeout conditions.
