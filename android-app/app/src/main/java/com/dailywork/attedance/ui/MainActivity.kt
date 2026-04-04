package com.dailywork.attedance.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dailywork.attedance.data.UserPreferencesRepository
import com.dailywork.attedance.ui.theme.DailyWorkTheme
import com.dailywork.attedance.viewmodel.AuthViewModel
import com.dailywork.attedance.viewmodel.CalendarViewModel
import com.dailywork.attedance.viewmodel.StatsViewModel
import com.dailywork.attedance.viewmodel.DashboardViewModel
import com.dailywork.attedance.viewmodel.PassbookViewModel
import com.dailywork.attedance.viewmodel.SettingsViewModel
import com.dailywork.attedance.viewmodel.WorkersViewModel
import com.dailywork.attedance.viewmodel.WorkerDetailViewModel
import com.dailywork.attedance.viewmodel.WorkerHistoryViewModel
import com.dailywork.attedance.viewmodel.ViewModelFactory
import com.dailywork.attedance.data.DataMigrationManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = UserPreferencesRepository(applicationContext)
        val factory = ViewModelFactory(repository)

        // Trigger Migration
        lifecycleScope.launch {
            val migrationManager = DataMigrationManager()
            if (!migrationManager.isMigrationCompleted()) {
                migrationManager.migrate()
            }
            // migrationManager.cleanupOldCollections() // Safe cleanup after verification
        }

        setContent {
            val isDarkMode by repository.darkModeFlow.collectAsState(initial = false)
            DailyWorkTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DailyWorkApp(factory)
                }
            }
        }
    }
}

@Composable
fun DailyWorkApp(factory: ViewModelFactory) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val calendarViewModel: CalendarViewModel = viewModel(factory = factory)
    val statsViewModel: StatsViewModel = viewModel(factory = factory)
    val passbookViewModel: PassbookViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val workersViewModel: WorkersViewModel = viewModel(factory = factory)
    val workerDetailViewModel: WorkerDetailViewModel = viewModel(factory = factory)
    val workerHistoryViewModel: WorkerHistoryViewModel = viewModel(factory = factory)

    // Using null for loading state, empty string for not set, actual value for set
    val tokenState by authViewModel.authTokenFlow.collectAsState(initial = "LOADING")
    val languageState by authViewModel.repository.languageFlow.collectAsState(initial = "LOADING")
    val roleState by authViewModel.userRoleFlow.collectAsState(initial = "LOADING")

    if (tokenState == "LOADING" || languageState == "LOADING" || roleState == "LOADING") {
        return // Wait for datastore
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    if (languageState == null) {
                        navController.navigate("language_selection") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else if (tokenState == null) {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else if (roleState == null || roleState!!.isEmpty()) {
                        navController.navigate("role_selection") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("language_selection") {
            LanguageSelectionScreen(
                repository = authViewModel.repository,
                onLanguageSelected = {
                    navController.navigate("login") {
                        popUpTo("language_selection") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                selectedLanguage = languageState ?: "en",
                onLoginSuccess = {
                    // Check if role is already fetched and saved in DataStore
                    if (roleState != null && roleState!!.isNotEmpty()) {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("role_selection") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("role_selection") {
            RoleSelectionScreen(
                authViewModel = authViewModel,
                onComplete = {
                    navController.navigate("dashboard") {
                        popUpTo("role_selection") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                navController = navController,
                dashboardViewModel = dashboardViewModel,
                calendarViewModel = calendarViewModel,
                statsViewModel = statsViewModel,
                passbookViewModel = passbookViewModel,
                settingsViewModel = settingsViewModel,
                workersViewModel = workersViewModel,
                workerDetailViewModel = workerDetailViewModel,
                workerHistoryViewModel = workerHistoryViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable("premium") {
            PremiumScreen(
                navController = navController,
                dashboardViewModel = dashboardViewModel
            )
        }
    }
}
