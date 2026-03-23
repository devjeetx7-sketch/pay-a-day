package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.viewmodel.DashboardViewModel
import com.dailywork.attedance.viewmodel.DashboardState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun DashboardScreen(dashboardViewModel: DashboardViewModel, navController: androidx.navigation.NavController, onLogout: () -> Unit) {
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    Scaffold(
        bottomBar = {
            BottomNavigationBar(role = dashboardState.role, currentRoute = currentRoute, onNavigate = { route ->
                bottomNavController.navigate(route) {
                    popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
        }
    ) { padding ->
        NavHost(navController = bottomNavController, startDestination = "dashboard", modifier = Modifier.fillMaxSize().padding(padding)) {
            composable("dashboard") {
                if (dashboardState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            HeaderSection(dashboardState)
                        }

                        if (dashboardState.role == "contractor") {
                            item { ContractorStatsGrid(dashboardState) }
                            item { ContractorQuickActions() }
                        } else {
                            item { PersonalStatsGrid(dashboardState) }
                            item { PersonalDailyLog(dashboardState) }
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
            composable("calendar") {
                val calendarViewModel: com.dailywork.attedance.viewmodel.CalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.dailywork.attedance.viewmodel.ViewModelFactory(
                        com.dailywork.attedance.data.UserPreferencesRepository(androidx.compose.ui.platform.LocalContext.current)
                    )
                )
                CalendarScreen(viewModel = calendarViewModel)
            }
            composable("settings") {
                SettingsScreen(onLogout = onLogout)
            }
            composable("workers") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Workers Screen") }
            }
            composable("stats") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Stats Screen") }
            }
            composable("passbook") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Passbook Screen") }
            }
        }
    }
}

@Composable
fun HeaderSection(state: DashboardState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.name.take(2).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hi, ${state.name.split(" ")[0]}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "👋", fontSize = 24.sp)
                }
                Text(
                    text = if (state.role == "contractor") "Manage your workforce" else "Manage your daily work",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Premium Crown Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEF3C7)) // amber-100
                .border(1.dp, Color(0xFFFDE68A), CircleShape), // amber-200
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium", tint = Color(0xFFD97706)) // amber-600
        }
    }
}

@Composable
fun ContractorStatsGrid(state: DashboardState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Mini Preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Weekly Performance", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("+12% active workers vs last week", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("View", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp))
        }

        // Stats Cards
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Total Workers", state.totalWorkers, Icons.Default.People, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatCard("Today's Attendance", state.todayPresent, Icons.Default.CheckCircle, Color(0xFF10B981), Modifier.weight(1f)) // green-500
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Total Paid (Month)", "₹${state.totalPaidMonth}", Icons.Default.AccountBalanceWallet, Color(0xFF3B82F6), Modifier.weight(1f)) // blue-500
            StatCard("Pending Amount", "₹${state.pendingAmount}", Icons.Default.AccountBalanceWallet, Color(0xFFF97316), Modifier.weight(1f), isPending = true) // orange-500
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, isPending: Boolean = false) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (isPending) color else MaterialTheme.colorScheme.onBackground)
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ContractorQuickActions() {
    Column {
        Text("QUICK ACTIONS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))

        QuickActionItem("Manage Workers", "Add, edit or remove workers", Icons.Default.People, MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        QuickActionItem("Mark Attendance", "Daily attendance for all workers", Icons.Default.CheckCircle, Color(0xFF10B981))
        Spacer(modifier = Modifier.height(16.dp))
        QuickActionItem("Add Advance Payment", "Record payments for workers", Icons.Default.CurrencyRupee, Color(0xFFF97316))
    }
}

@Composable
fun QuickActionItem(title: String, subtitle: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun PersonalStatsGrid(state: DashboardState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(20.dp)
            ) {
                Text("Today's Earnings", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("₹${state.todayEarned}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(20.dp)
            ) {
                Text("Monthly Earnings", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("₹${state.monthEarned}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("View My Passbook", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun PersonalDailyLog(state: DashboardState) {
    Column {
        Text("DAILY LOG", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))

        if (state.todayStatus == null) {
            // Overtime Input (Simplified for UI representation)
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Overtime (Hours)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    Text("0", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Note Input
            OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("Add Note...") }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline))
            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {}, modifier = Modifier.weight(1f).height(100.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Full Day", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Button(onClick = {}, modifier = Modifier.weight(1f).height(100.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Half Day", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark Absent", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
             Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                     Spacer(modifier = Modifier.height(12.dp))
                     Text("Marked Present", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                 }
             }
        }
    }
}

@Composable
fun BottomNavigationBar(role: String, currentRoute: String, onNavigate: (String) -> Unit = {}) {
    val isContractor = role == "contractor"

    data class NavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

    val tabs = mutableListOf(
        NavItem("dashboard", Icons.Default.Home, "Dashboard"),
        NavItem("calendar", Icons.Default.CalendarMonth, "Calendar")
    )

    if (isContractor) {
        tabs.add(NavItem("workers", Icons.Default.People, "Workers"))
    } else {
        tabs.add(NavItem("passbook", Icons.Default.Description, "Passbook"))
    }

    tabs.add(NavItem("stats", Icons.Default.BarChart, "Stats"))
    tabs.add(NavItem("settings", Icons.Default.Settings, "Settings"))

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == currentRoute
            NavigationBarItem(
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                selected = selected,
                onClick = { onNavigate(tab.route) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.background,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
