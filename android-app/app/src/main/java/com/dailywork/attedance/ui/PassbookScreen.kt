package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import android.content.Intent
import android.net.Uri
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
    navController: NavController
) {
    val state by viewModel.state.collectAsState()
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

        val sortedLogs = state.logs.sortedBy { it.date }.map { log ->
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

    fun exportPDF() {
        val file = PassbookPdfGenerator.generatePdf(context, generatePdfData())
        if (file != null) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF"))
        }
    }

    fun shareViaWhatsApp() {
        val text = """
            👷 *Official Worker Passbook*

            👤 *Name:* ${state.name}
            🛠️ *Role:* ${state.workType}
            📅 *Month:* $monthYearStr
            📊 *Attendance Summary:*
            ✅ Present: ${state.presentDays} days
            🕒 Half Days: ${state.halfDays} days
            ❌ Absent: ${state.absentDays} days
            📈 *Total Man Days:* ${state.totalDailyWorks}

            💰 *Financial Summary:*
            💵 Daily Wage: ₹${state.dailyWage.toInt()}
            💸 Gross Earned: ₹${state.grossEarned.toInt()}
            📉 Advance Deducted: ₹${state.totalAdvance.toInt()}
            ━━━━━━━━━━━━━━━━━━
            🏆 *Net Payable: ₹${state.finalBalance.toInt()}*
            ━━━━━━━━━━━━━━━━━━

            _Generated automatically via DailyWork Pro App_ 📱
        """.trimIndent()

        val file = PassbookPdfGenerator.generatePdf(context, generatePdfData())
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            if (file != null) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback if WhatsApp is not installed
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                if (file != null) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Share PDF"))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(state.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(state.workType, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.workType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("₹${state.dailyWage.toInt()} / day", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF16A34A))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Joined ${state.joinedDate}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { navController.navigate("calendar") }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(onClick = { navController.navigate("calendar") }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Advance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Passbook Month", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", modifier = Modifier.size(20.dp)) }
                    Text(text = monthYearStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(20.dp)) }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(
                    onClick = {
                        if (state.isPremium) {
                            exportPDF()
                        } else {
                            navController.navigate("premium")
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(if (state.isPremium) Icons.Default.PictureAsPdf else Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (state.isPremium) {
                            shareViaWhatsApp()
                        } else {
                            navController.navigate("premium")
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(if (state.isPremium) Icons.Default.Share else Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attendance Rate", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("${state.attendanceRate}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(progress = state.attendanceRate / 100f, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Worked ${state.totalDailyWorks} out of ${state.passedDays} passed days this month", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.presentDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        Text("PRESENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.absentDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        Text("ABSENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.halfDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                        Text("HALF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.totalDailyWorks}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("MAN DAYS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Financial Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Gross Earned", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Text("₹${state.grossEarned.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Advance", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Text("- ₹${state.totalAdvance.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Net Payable", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("₹${state.finalBalance.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (state.finalBalance >= 0) MaterialTheme.colorScheme.primary else Color(0xFFDC2626))
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Detailed Ledger", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (state.logs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No records found for this month.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(state.logs) { log ->
                val dateParts = log.date.split("-").reversed()
                val displayDate = if (dateParts.size == 3) "${dateParts[0]}/${dateParts[1]}/${dateParts[2]}" else log.date

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(displayDate, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (!log.note.isNullOrEmpty()) {
                            Text(log.note, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (log.status != "advance") {
                            val bgColor = if (log.status == "present") Color(0xFF22C55E).copy(alpha=0.1f) else Color(0xFFEF4444).copy(alpha=0.1f)
                            val textColor = if (log.status == "present") Color(0xFF16A34A) else Color(0xFFDC2626)
                            val text = if (log.status == "present") { if (log.type == "half") "Half Day" else "Present" } else "Absent"

                            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(bgColor).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                        if (log.advanceAmount != null && log.advanceAmount > 0) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF97316).copy(alpha=0.1f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("₹${log.advanceAmount.toInt()} Advance", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                            }
                        }
                    }
                }
            }
        }
            }
        }
        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}
