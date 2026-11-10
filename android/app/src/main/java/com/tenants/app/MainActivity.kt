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
    data object TenantDetails : Screen("tenant_details/{tenantId}", R.string.nav_tenants, Icons.Default.People) {
        fun createRoute(tenantId: String) = "tenant_details/$tenantId"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TenantsTheme {
                val authViewModel: AuthViewModel = koinViewModel()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val bottomNavItems = listOf(Screen.Dashboard, Screen.Rooms, Screen.Tenants, Screen.Payments)

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = authState.isLoggedIn && currentRoute != Screen.Login.route && currentRoute?.startsWith("tenant_details") != true

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
                        modifier = Modifier.padding(paddingValues)
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
                        composable(Screen.Dashboard.route) { DashboardScreen() }
                        composable(Screen.Rooms.route) { RoomsScreen() }
                        composable(Screen.Tenants.route) {
                            TenantsScreen(
                                onTenantClick = { tenantId ->
                                    navController.navigate(Screen.TenantDetails.createRoute(tenantId))
                                }
                            )
                        }
                        composable(Screen.Payments.route) { PaymentScreen() }
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
