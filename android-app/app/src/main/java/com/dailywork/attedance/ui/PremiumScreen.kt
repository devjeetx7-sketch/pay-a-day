package com.dailywork.attedance.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailywork.attedance.viewmodel.DashboardViewModel

data class Plan(val id: String, val label: String, val price: Int, val tag: String? = null)
data class Feature(val name: String, val freeDesc: String, val isPremium: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel
) {
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()

    // Toggle state for Personal vs Contractor features comparison
    var selectedTab by remember { mutableStateOf("personal") }

    // Pricing values must remain EXACTLY the same
    val basePrice = 49 // Hardcoded personal price base

    val plans = listOf(
        Plan("monthly", "Monthly", basePrice),
        Plan("half-yearly", "6 Months", basePrice * 5, "Save 16%"),
        Plan("yearly", "Yearly", basePrice * 10, "Save 16%"),
        Plan("lifetime", "Lifetime", basePrice * 20, "Best Value")
    )

    var selectedPlanId by remember { mutableStateOf("lifetime") }
    val selectedPlan = plans.find { it.id == selectedPlanId } ?: plans.last()

    val personalFeatures = listOf(
        Feature("Basic Attendance", "✔", true),
        Feature("Overtime Tracking", "Limited", true),
        Feature("Theme Support", "Limited", true),
        Feature("Unlimited passbook history", "—", true),
        Feature("PDF export", "—", true),
        Feature("Cloud backup & restore", "—", true),
        Feature("Advanced analytics", "—", true),
        Feature("Worker history", "—", true),
        Feature("Multiple role support", "—", true),
        Feature("Faster sync", "—", true),
        Feature("Priority updates", "—", true)
    )

    val contractorFeatures = listOf(
        Feature("Unlimited workers", "—", true),
        Feature("Worker attendance history", "—", true),
        Feature("Contractor analytics", "—", true),
        Feature("Payment history", "—", true),
        Feature("PDF reports", "—", true),
        Feature("Cloud backup", "—", true),
        Feature("Worker passbook export", "—", true),
        Feature("Advanced dashboard", "—", true),
        Feature("Statistics", "—", true),
        Feature("Multi-worker overtime tracking", "—", true)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DailyWork Premium", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Premium Hero Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFFFFB75E), Color(0xFFED8F03)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Upgrade to Premium",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unlock advanced tools, secure backups, and powerful work management features",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Tab Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    val personalColor by animateColorAsState(if (selectedTab == "personal") MaterialTheme.colorScheme.primary else Color.Transparent)
                    val personalTextColor by animateColorAsState(if (selectedTab == "personal") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(personalColor)
                            .clickable { selectedTab = "personal" }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Personal Premium", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = personalTextColor)
                    }

                    val contractorColor by animateColorAsState(if (selectedTab == "contractor") MaterialTheme.colorScheme.primary else Color.Transparent)
                    val contractorTextColor by animateColorAsState(if (selectedTab == "contractor") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(contractorColor)
                            .clickable { selectedTab = "contractor" }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Contractor Premium", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = contractorTextColor)
                    }
                }
            }

            // Feature Comparison Table
            item {
                Text(
                    "Compare Free vs Premium",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Feature", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Free", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Premium", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        val activeFeatures = if (selectedTab == "personal") personalFeatures else contractorFeatures

                        activeFeatures.forEachIndexed { index, feature ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(feature.name, modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(feature.freeDesc, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (feature.isPremium) "✔" else "—", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            }
                            if (index < activeFeatures.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // Plans
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    plans.forEach { plan ->
                        PremiumPlanCard(
                            plan = plan,
                            isSelected = selectedPlanId == plan.id,
                            onClick = { selectedPlanId = plan.id }
                        )
                    }
                }
            }

            // CTA Button
            item {
                Button(
                    onClick = { /* Implement Upgrade */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp), clip = false),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Upgrade for ₹${selectedPlan.price}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumPlanCard(plan: Plan, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    val bgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface)
    val elevation by animateDpAsState(if (isSelected) 6.dp else 2.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            // Selected Check Icon
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .offset(x = 4.dp, y = (-4).dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        plan.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (plan.tag == "Best Value") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFF59E0B))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("⭐ Best Value", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else if (plan.tag != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(plan.tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text("₹${plan.price}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    if (plan.id != "lifetime") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("/${plan.label.lowercase()}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
