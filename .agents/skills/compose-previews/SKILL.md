---
name: compose-previews
description: Require standalone @Preview methods with realistic mock/default state parameters for every screen and reusable composable component.
---

# Jetpack Compose Preview Standard Skill

This skill ensures every UI component can be immediately viewed and verified in the Android Studio visual design editor without running an emulator or device.

## 1. Mandatory Preview Requirement
Every UI file that exposes a screen or reusable component must include at least one `@Preview` composable method at the bottom of the file.

## 2. Stateless Content Previews
Because ViewModels and network/DI instances cannot run inside Android Studio design previews, always preview the stateless `*Content` composable using sample data.

### Standard Template:
```kotlin
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun PaymentContentPreview() {
    TenantsTheme {
        PaymentContent(
            state = PaymentUiState(
                isLoading = false,
                tenants = listOf(
                    Tenant(id = "1", name = "Rahul Sharma", roomNo = "101", balance = -2500.0)
                ),
                selectedTenant = null,
                rentAmount = "5000",
                electricityUnit = "120"
            ),
            onEvent = {}
        )
    }
}
```

## 3. Coverage of Edge States
For critical screens (Dashboard, Payment, Tenant Details), provide preview variants for:
- **Default / Populated State**
- **Empty State** (No tenants, no rooms)
- **Loading / Skeleton State**
- **Error / Banner State**
