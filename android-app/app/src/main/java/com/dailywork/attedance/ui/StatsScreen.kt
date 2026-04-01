package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailywork.attedance.viewmodel.StatsViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreenContent(
    viewModel: StatsViewModel
) {
    val state by viewModel.statsState.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
        }
    }
    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            pullRefreshState.startRefresh()
        } else {
            pullRefreshState.endRefresh()
        }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Monthly", "All-Time")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            if (state.isLoading && state.role.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (state.role == "contractor") {
                    ContractorStatsView(viewModel = viewModel, state = state, isAllTime = selectedTabIndex == 1)
                } else {
                    PersonalStatsView(viewModel = viewModel, state = state, isAllTime = selectedTabIndex == 1)
                }
            }
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ContractorStatsView(viewModel: StatsViewModel, state: com.dailywork.attedance.viewmodel.StatsState, isAllTime: Boolean) {
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearStr = sdfMonth.format(state.selectedMonthDate)

    val currentTotalCost = if (isAllTime) state.contractorStats.allTimeCost else state.contractorStats.totalCost
    val currentTotalDays = if (isAllTime) state.contractorStats.allTimeWorks else state.contractorStats.totalDailyWorks
    val currentTopWorkers = if (isAllTime) state.contractorStats.allTimeTopWorkers else state.contractorStats.topWorkers

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Global Statistics", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Worker performance & costs", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            if (!isAllTime) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous") }
                    Text(text = monthYearStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text("TOTAL LABOUR COST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${currentTotalCost.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text("TOTAL MAN DAYS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$currentTotalDays", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        item {
            val maxCost = currentTopWorkers.maxOfOrNull { it.cost } ?: 1.0

            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                Column {
                    Text("Cost Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentTopWorkers.isEmpty()) {
                        Text("No active workers ${if (isAllTime) "found" else "this month"}.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
                    } else {
                        currentTopWorkers.take(5).forEach { worker ->
                            val progress = (worker.cost / maxCost).toFloat()
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(1000)
                            )

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("₹${worker.cost.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${worker.days}d", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalStatsView(viewModel: StatsViewModel, state: com.dailywork.attedance.viewmodel.StatsState, isAllTime: Boolean) {
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearStr = sdfMonth.format(state.selectedMonthDate)

    val currentEarnings = if (isAllTime) state.personalStats.allTimeEarnings else state.personalStats.totalEarnings
    val currentDays = if (isAllTime) state.personalStats.allTimeDays else (state.personalStats.present + state.personalStats.halfDays)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Stats", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))

            if (!isAllTime) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous") }
                    Text(text = monthYearStr, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text("EARNINGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${currentEarnings.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text("TOTAL DAYS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$currentDays", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text("TOTAL OVERTIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${state.personalStats.overtime} hrs", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text("ADVANCE DEDUCTIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${state.personalStats.advanceTotal.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                    }
                }
            }
        }

        item {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text("Attendance Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(24.dp))

                        val total = (state.personalStats.present + state.personalStats.absent + state.personalStats.halfDays).toFloat()
                        if (total > 0) {
                            val presentSweep = (state.personalStats.present / total) * 360f
                            val absentSweep = (state.personalStats.absent / total) * 360f
                            val halfSweep = (state.personalStats.halfDays / total) * 360f

                            val animatedPresentSweep by animateFloatAsState(targetValue = presentSweep, animationSpec = tween(1000))
                            val animatedAbsentSweep by animateFloatAsState(targetValue = absentSweep, animationSpec = tween(1000))
                            val animatedHalfSweep by animateFloatAsState(targetValue = halfSweep, animationSpec = tween(1000))

                            val presentColor = MaterialTheme.colorScheme.primary
                            val absentColor = MaterialTheme.colorScheme.error
                            val halfColor = Color(0xFFF97316)

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                    Canvas(modifier = Modifier.size(100.dp)) {
                                        val strokeWidth = 24.dp.toPx()
                                        drawArc(
                                            color = presentColor,
                                            startAngle = -90f,
                                            sweepAngle = animatedPresentSweep,
                                            useCenter = false,
                                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = absentColor,
                                            startAngle = -90f + animatedPresentSweep,
                                            sweepAngle = animatedAbsentSweep,
                                            useCenter = false,
                                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = halfColor,
                                            startAngle = -90f + animatedPresentSweep + animatedAbsentSweep,
                                            sweepAngle = animatedHalfSweep,
                                            useCenter = false,
                                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${((state.personalStats.present / total) * 100).toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        Text("Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.width(24.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(presentColor))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Present (${state.personalStats.present})", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(halfColor))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Half Day (${state.personalStats.halfDays})", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(absentColor))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Absent (${state.personalStats.absent})", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No records found.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                    }
                }
            }
        }

        item {
            Text("All Time Stats", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text("TOTAL EARNINGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${state.personalStats.allTimeEarnings.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text("TOTAL DAYS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${state.personalStats.allTimeDays}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    }
}
