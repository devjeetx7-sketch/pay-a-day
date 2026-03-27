package com.dailywork.attedance.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.dailywork.attedance.utils.PassbookPdfGenerator
import com.dailywork.attedance.utils.PdfData
import com.dailywork.attedance.utils.PdfLog
import com.dailywork.attedance.viewmodel.PassbookViewModel
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassbookScreenContent(
    viewModel: PassbookViewModel,
    navController: NavController,
    onMenuClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val logs = viewModel.logsFlow.collectAsLazyPagingItems()
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val sdfMonthNumeric = SimpleDateFormat("MM", Locale.getDefault())
    val sdfYearNumeric = SimpleDateFormat("yyyy", Locale.getDefault())
    val monthYearStr = sdfMonth.format(state.selectedMonthDate)
    val monthNumericStr = sdfMonthNumeric.format(state.selectedMonthDate)
    val yearNumericStr = sdfYearNumeric.format(state.selectedMonthDate)
    val context = LocalContext.current

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

    fun generatePdfData(): PdfData {
        var currentRunningBalance = 0.0

        val loadedLogs = mutableListOf<com.dailywork.attedance.viewmodel.PassbookLog>()
        for (i in 0 until logs.itemCount) {
            logs[i]?.let { loadedLogs.add(it) }
        }

        val sortedLogs = loadedLogs.sortedBy { it.date }.map { log ->
            val dailyEarned = if (log.status == "present") {
                if (log.type == "half") state.dailyWage / 2 else state.dailyWage
            } else 0.0
            val advanceAmt = log.advanceAmount ?: 0.0

            currentRunningBalance += dailyEarned - advanceAmt

            PdfLog(
                date = log.date.split("-").reversed().joinToString("/"),
                status = if (log.status == "present") if (log.type == "half") "Half Day" else "Present" else if (log.status == "absent") "Absent" else "-",
                workType = state.workType,
                dailyWage = "Rs. ${state.dailyWage.toInt()}",
                overtime = "-",
                advanceAmount = if (log.status == "advance" || advanceAmt > 0) "Rs. ${advanceAmt.toInt()}" else "-",
                runningBalance = "Rs. ${currentRunningBalance.toInt()}"
            )
        }

        return PdfData(
            title = "DailyWork Pro",
            subtitle = "Official Worker Passbook",
            name = state.name,
            userId = "personal", // No specific worker ID in personal passbook view
            role = state.workType,
            monthYearStr = monthYearStr,
            monthNumericStr = monthNumericStr,
            yearNumericStr = yearNumericStr,
            contractorName = "Admin", // PassbookViewModel doesn't fetch contractor name for personal mode
            dailyWage = state.dailyWage,
            totalManDays = state.totalDailyWorks,
            grossEarned = state.grossEarned,
            advanceDeducted = state.totalAdvance,
            netPayable = state.finalBalance,
            logs = sortedLogs
        )
    }


    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullRefreshState.nestedScrollConnection)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Text("Passbook", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }

                Row {
                    IconButton(
                        onClick = {
                            if (!state.isPremium) {
                                navController.navigate("premium")
                                return@IconButton
                            }

                            val title = "Personal Passbook - ${monthYearStr}"
                            val pdfFile = PassbookPdfGenerator.generatePdf(
                                context = context,
                                data = generatePdfData()
                            )
                            if (pdfFile != null) {
                                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Passbook for ${state.name}")
                                    putExtra(Intent.EXTRA_TEXT, "Please find attached the passbook for $monthYearStr.")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    setPackage("com.whatsapp")
                                }

                                try {
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    // Fallback if WhatsApp is not installed
                                    val fallbackIntent = Intent.createChooser(shareIntent, "Share PDF")
                                    context.startActivity(fallbackIntent)
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share WhatsApp", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (!state.isPremium) {
                                navController.navigate("premium")
                                return@IconButton
                            }

                            val title = "Personal Passbook - ${monthYearStr}"
                            val pdfFile = PassbookPdfGenerator.generatePdf(
                                context = context,
                                data = generatePdfData()
                            )
                            if (pdfFile != null) {
                                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(viewIntent, "Open PDF"))
                            }
                        },
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Month Selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeMonth(-1) }) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Month", tint = MaterialTheme.colorScheme.primary)
                }
                Text(monthYearStr, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                IconButton(onClick = { viewModel.changeMonth(1) }) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Profile Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(state.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(state.workType, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Daily Wage", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("₹${state.dailyWage.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Joined", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(state.joinedDate, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    // Attendance Summary
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Attendance Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(state.presentDays.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF39b27d))
                                        Text("Present", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(state.absentDays.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        Text("Absent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(state.halfDays.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                                        Text("Half Day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Attendance Rate (${state.totalDailyWorks} / ${state.passedDays} days)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${state.attendanceRate}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { state.attendanceRate / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }

                    // Financial Summary
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Financial Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Gross Earned", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("₹${state.grossEarned.toInt()}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Advance", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("- ₹${state.totalAdvance.toInt()}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Final Balance", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("₹${state.finalBalance.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detailed Ledger", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    if (logs.itemCount == 0 && !state.isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No records found for this month.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        items(count = logs.itemCount) { index ->
                            val log = logs[index] ?: return@items
                            val dateParts = log.date.split("-").reversed()
                            val displayDate = if (dateParts.size == 3) "${dateParts[0]}/${dateParts[1]}/${dateParts[2]}" else log.date

                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(displayDate, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (!log.note.isNullOrEmpty()) {
                                        Text(log.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (log.status == "present") if (log.type == "half") "Half Day" else "Present" else if (log.status == "absent") "Absent" else "Advance",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (log.status == "present") Color(0xFF39b27d) else if (log.status == "absent") MaterialTheme.colorScheme.error else Color(0xFFF97316)
                                    )
                                    if ((log.advanceAmount ?: 0.0) > 0) {
                                        Text("Adv: ₹${log.advanceAmount?.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullRefreshState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}
