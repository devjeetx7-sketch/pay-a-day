package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailywork.attedance.viewmodel.AttendanceRecord
import com.dailywork.attedance.viewmodel.CalendarViewModel
import com.dailywork.attedance.viewmodel.Worker
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    navController: NavController
) {
    val state by viewModel.calendarState.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                role = state.role,
                currentRoute = "calendar",
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.role.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (state.role == "contractor") {
                    ContractorCalendarView(viewModel = viewModel, state = state)
                } else {
                    PersonalCalendarView(viewModel = viewModel, state = state)
                }
            }
        }
    }
}

@Composable
fun ContractorCalendarView(viewModel: CalendarViewModel, state: com.dailywork.attedance.viewmodel.CalendarState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Mark Attendance", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Daily attendance for all workers", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // Date Picker/Selector Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.changeContractorDate(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.selectedDate,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = { viewModel.changeContractorDate(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.workers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No workers found. Add workers first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.workers) { worker ->
                    WorkerAttendanceCard(
                        worker = worker,
                        attendance = state.contractorAttendance.find { it.userId == "worker_${worker.id}" && it.status != "advance" },
                        onMarkAttendance = { status, type ->
                            viewModel.markContractorAttendance(worker.id, status, type)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkerAttendanceCard(
    worker: Worker,
    attendance: AttendanceRecord?,
    onMarkAttendance: (String, String) -> Unit
) {
    val currentStatus = attendance?.status
    val currentType = attendance?.type

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = worker.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = worker.workType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Present (Full Day)
                val isPresentFull = currentStatus == "present" && currentType == "full"
                OutlinedButton(
                    onClick = { onMarkAttendance("present", "full") },
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isPresentFull) Color(0xFF22C55E) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isPresentFull) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPresentFull) Color(0xFF22C55E) else MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Present", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Half Day
                val isPresentHalf = currentStatus == "present" && currentType == "half"
                OutlinedButton(
                    onClick = { onMarkAttendance("present", "half") },
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isPresentHalf) Color(0xFF86EFAC) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isPresentHalf) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPresentHalf) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Half Day", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Absent
                val isAbsent = currentStatus == "absent"
                OutlinedButton(
                    onClick = { onMarkAttendance("absent", "full") },
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isAbsent) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Absent", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalCalendarView(viewModel: CalendarViewModel, state: com.dailywork.attedance.viewmodel.CalendarState) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    val currentMonthDate = state.currentMonthDate
    val cal = Calendar.getInstance()
    cal.time = currentMonthDate
    val month = cal.get(Calendar.MONTH)
    val year = cal.get(Calendar.YEAR)

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1

    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearStr = sdfMonth.format(currentMonthDate)

    val todayCal = Calendar.getInstance()
    val isCurrentMonth = todayCal.get(Calendar.YEAR) == year && todayCal.get(Calendar.MONTH) == month
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // Group attendance by day
    val dayMap = remember(state.personalAttendance) {
        val map = mutableMapOf<Int, AttendanceRecord>()

        state.personalAttendance.forEach { record ->
            val recordParts = record.date.split("-")
            if (recordParts.size == 3) {
                val rYear = recordParts[0].toIntOrNull() ?: return@forEach
                val rMonth = (recordParts[1].toIntOrNull() ?: return@forEach) - 1
                val rDay = recordParts[2].toIntOrNull() ?: return@forEach

                if (rYear == year && rMonth == month) {
                    map[rDay] = record
                }
            }
        }
        map
    }

    val presentCount = dayMap.values.count { it.status == "present" }
    val absentCount = dayMap.values.count { it.status == "absent" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Calendar", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))

        // Month Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.changePersonalMonth(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }
            Text(
                text = monthYearStr,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.changePersonalMonth(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Month stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$presentCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Present", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$absentCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Absent", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tap any day to view details.",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Day Labels
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            dayLabels.forEach { d ->
                Text(
                    text = d,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = Math.ceil(totalCells / 7.0).toInt()

        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - firstDayOfWeek + 1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day in 1..daysInMonth) {
                                val data = dayMap[day]
                                val isPresent = data?.status == "present"
                                val isAbsent = data?.status == "absent"
                                val isHalf = data?.type == "half"
                                val isToday = isCurrentMonth && day == todayDay

                                val bgColor = when {
                                    isPresent && isHalf -> Color(0xFF86EFAC)
                                    isPresent -> MaterialTheme.colorScheme.primary
                                    isAbsent -> MaterialTheme.colorScheme.error
                                    else -> Color.Transparent
                                }

                                val textColor = when {
                                    isPresent || isAbsent -> Color.White
                                    isToday -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = androidx.compose.material.ripple.rememberRipple()
                                        ) {
                                            if (data != null) {
                                                selectedDay = day
                                                showDialog = true
                                            }
                                        }
                                        .border(
                                            width = if (isToday && !isPresent && !isAbsent) 2.dp else 0.dp,
                                            color = if (isToday && !isPresent && !isAbsent) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Full Day", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF86EFAC)))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Half Day", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Absent", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showDialog && selectedDay != null) {
        val existingRecord = dayMap[selectedDay!!]

        PersonalDetailsDialog(
            displayDate = "$selectedDay ${SimpleDateFormat("MMMM", Locale.getDefault()).format(currentMonthDate)} $year",
            existingRecord = existingRecord,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun PersonalDetailsDialog(
    displayDate: String,
    existingRecord: AttendanceRecord?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(displayDate, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (existingRecord?.status == "present") {
                    val typeCap = existingRecord.type?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Full"
                    Text("Status: Present ($typeCap Day)", fontWeight = FontWeight.SemiBold)
                    if ((existingRecord.overtimeHours ?: 0) > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Overtime: ${existingRecord.overtimeHours} Hours")
                    }
                } else if (existingRecord?.status == "absent") {
                    Text("Status: Absent", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }

                if (!existingRecord?.note.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Note:", fontWeight = FontWeight.SemiBold)
                    Text(existingRecord?.note ?: "")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}
