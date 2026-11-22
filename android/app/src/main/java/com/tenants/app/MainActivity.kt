package com.tenants.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tenants.app.presentation.auth.AuthViewModel
import com.tenants.app.presentation.auth.LoginScreen
import com.tenants.app.presentation.dashboard.DashboardScreen
import com.tenants.app.presentation.payments.PaymentScreen
import com.tenants.app.presentation.rooms.RoomsScreen
import com.tenants.app.presentation.tenants.TenantDetailsScreen
import com.tenants.app.presentation.tenants.TenantsScreen
import com.tenants.app.presentation.transactions.TransactionsScreen
import com.tenants.app.presentation.theme.Primary
import com.tenants.app.presentation.theme.Surface
import com.tenants.app.presentation.theme.TenantsTheme
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String, val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Login : Screen("login", R.string.login_title, Icons.Default.Dashboard)
    data object Dashboard : Screen("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard)
    data object Rooms : Screen("rooms", R.string.nav_rooms, Icons.Default.MeetingRoom)
    data object Tenants : Screen("tenants", R.string.nav_tenants, Icons.Default.People)
    data object Payments : Screen("payments", R.string.nav_payments, Icons.Default.Payment)
    data object Transactions : Screen("transactions", R.string.all_transactions_title, Icons.Default.Payment)
    data object AddTenant : Screen("add_tenant", R.string.add_tenant_title, Icons.Default.People)
    data object MeterBilling : Screen("meter_billing", R.string.meter_readings_title, Icons.Default.MeetingRoom)
    data object TenantDetails : Screen("tenant_details/{tenantId}", R.string.nav_tenants, Icons.Default.People) {
        fun createRoute(tenantId: String) = "tenant_details/$tenantId"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set status bar icons and background cleanly
        window.statusBarColor = android.graphics.Color.parseColor("#F3FCF4")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        setContent {
            TenantsTheme {
                val authViewModel: AuthViewModel = koinViewModel()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val bottomNavItems = listOf(Screen.Dashboard, Screen.Rooms, Screen.Tenants, Screen.Payments)

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = authState.isLoggedIn &&
                    currentRoute != Screen.Login.route &&
                    currentRoute != Screen.AddTenant.route &&
                    currentRoute != Screen.MeterBilling.route &&
                    currentRoute != Screen.Transactions.route &&
                    currentRoute?.startsWith("tenant_details") != true && currentRoute != Screen.Transactions.route

                // Listen for Forced Logout event (e.g. 7-day token expired)
                val context = androidx.compose.ui.platform.LocalContext.current
                LaunchedEffect(Unit) {
                    com.tenants.app.data.local.SessionEvents.forceLogoutEvent.collect { reason ->
                        android.widget.Toast.makeText(context, reason, android.widget.Toast.LENGTH_LONG).show()
                        authViewModel.onEvent(com.tenants.app.presentation.auth.AuthEvent.Logout)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(containerColor = Surface) {
                                bottomNavItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                                        label = { Text(stringResource(screen.titleRes)) },
                                        selected = currentRoute == screen.route,
                                        colors = NavigationBarItemDefaults.colors(indicatorColor = Primary.copy(alpha = 0.15f)),
                                        onClick = {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    val startRoute = if (authState.isLoggedIn) Screen.Dashboard.route else Screen.Login.route
                    NavHost(
                        navController = navController,
                        startDestination = startRoute,
                        modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                onLogoutClick = {
                                    authViewModel.onEvent(com.tenants.app.presentation.auth.AuthEvent.Logout)
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onNavigateToTransactions = {
                                    navController.navigate(Screen.Transactions.route)
                                },
                                onNavigateToMeterBilling = {
                                    navController.navigate(Screen.MeterBilling.route)
                                }
                            )
                        }
                        composable(Screen.Rooms.route) { RoomsScreen() }
                        composable(Screen.Tenants.route) {
                            TenantsScreen(
                                onTenantClick = { tenantId ->
                                    navController.navigate(Screen.TenantDetails.createRoute(tenantId))
                                },
                                onNavigateToAddTenant = {
                                    navController.navigate(Screen.AddTenant.route)
                                }
                            )
                        }
                        composable(Screen.AddTenant.route) {
                            com.tenants.app.presentation.tenants.AddTenantScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.MeterBilling.route) {
                            com.tenants.app.presentation.billing.MeterBillingScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Payments.route) { PaymentScreen() }
                        composable(Screen.Transactions.route) {
                            TransactionsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.TenantDetails.route) { backStackEntry ->
                            val tenantId = backStackEntry.arguments?.getString("tenantId") ?: ""
                            TenantDetailsScreen(
                                tenantId = tenantId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
