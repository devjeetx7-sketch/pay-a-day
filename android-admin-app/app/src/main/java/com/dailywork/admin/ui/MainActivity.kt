package com.dailywork.admin.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dailywork.admin.ui.screens.*
import com.dailywork.admin.viewmodel.AuthState
import com.dailywork.admin.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AdminApp()
            }
        }
    }
}

@Composable
fun AdminApp() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    when (authState) {
        is AuthState.Authenticated -> MainScaffold(authViewModel)
        is AuthState.Loading -> LoadingScreen()
        else -> LoginScreen(authViewModel)
    }
}

@Composable
fun LoadingScreen() {
    Surface(modifier = androidx.compose.foundation.layout.fillMaxSize()) {
        androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun MainScaffold(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Users,
        Screen.Notifications,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.Dashboard.route) },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                text = { Text("Live Dashboard") }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Users.route, Modifier.padding(innerPadding)) {
        composable(Screen.Users.route) {
            UsersScreen(onNotifyUser = { userId ->
                navController.navigate("${Screen.Notifications.route}?userId=$userId")
            })
        }
        composable(
            route = "${Screen.Notifications.route}?userId={userId}",
            arguments = listOf(
                androidx.navigation.navArgument("userId") {
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            NotificationsScreen(targetUserId = userId)
        }
            composable(Screen.Settings.route) { SettingsScreen(onLogout = { authViewModel.logout() }) }
            composable(Screen.Dashboard.route) { LiveDashboardScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Users : Screen("users", "Users", Icons.Default.People)
    object Notifications : Screen("notifications", "Notify", Icons.Default.Notifications)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
}
