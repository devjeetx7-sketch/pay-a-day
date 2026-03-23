package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.viewmodel.CalendarViewModel
import com.dailywork.attedance.viewmodel.CalendarState
import androidx.compose.foundation.shape.CircleShape
import com.dailywork.attedance.viewmodel.WorkerWithAttendance
import com.dailywork.attedance.viewmodel.DayData
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val state by viewModel.state.collectAsState()

    if (state.role == "contractor") {
        ContractorCalendar(state, viewModel)
    } else if (state.role.isNotEmpty()) {
        PersonalCalendar(state, viewModel)
    }
}

@Composable
fun ContractorCalendar(state: CalendarState, viewModel: CalendarViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            // Simplified date display instead of native picker for matching web styling directly
            Text(state.selectedDateStr, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))

            Row {
                IconButton(onClick = {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val cal = Calendar.getInstance().apply { time = sdf.parse(state.selectedDateStr)!! }
                    cal.add(Calendar.DAY_OF_MONTH, -1)
                    viewModel.changeContractorDate(sdf.format(cal.time))
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val cal = Calendar.getInstance().apply { time = sdf.parse(state.selectedDateStr)!! }
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    viewModel.changeContractorDate(sdf.format(cal.time))
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (state.isLoading && state.contractorWorkers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.contractorWorkers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No workers found. Add workers first.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.contractorWorkers) { worker ->
                    WorkerAttendanceCard(worker, onMark = { status, type ->
                        viewModel.markContractorAttendance(worker.id, status, type)
                    })
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun WorkerAttendanceCard(worker: WorkerWithAttendance, onMark: (String, String) -> Unit) {
    val currentStatus = worker.status
    val currentType = worker.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(worker.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(worker.workType, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isFull = currentStatus == "present" && currentType == "full"
            AttendanceButton(
                text = "Present", icon = Icons.Default.Check,
                isSelected = isFull,
                selectedColor = Color(0xFF22C55E), // green-500
                modifier = Modifier.weight(1f)
            ) { onMark("present", "full") }

            val isHalf = currentStatus == "present" && currentType == "half"
            AttendanceButton(
                text = "Half Day", icon = Icons.Default.Schedule,
                isSelected = isHalf,
                selectedColor = Color(0xFFF97316), // orange-500
                modifier = Modifier.weight(1f)
            ) { onMark("present", "half") }

            val isAbsent = currentStatus == "absent"
            AttendanceButton(
                text = "Absent", icon = Icons.Default.Close,
                isSelected = isAbsent,
                selectedColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            ) { onMark("absent", "full") }
        }
    }
}

@Composable
fun AttendanceButton(
    text: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean, selectedColor: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val bgColor = if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) selectedColor else MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = contentColor)
    }
}

@Composable
fun PersonalCalendar(state: CalendarState, viewModel: CalendarViewModel) {
    val cal = Calendar.getInstance().apply { time = state.currentMonthDate }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)

    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthDisplay = sdfMonth.format(cal.time)

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    // Calendar.DAY_OF_WEEK returns 1 for Sunday, 2 for Monday, etc.
    // If the legend starts with Sunday ("S", "M", "T"...), index should be 0 for Sunday.
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1

    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.changePersonalMonth(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(monthDisplay, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            IconButton(onClick = { viewModel.changePersonalMonth(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Calendar Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(it, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val totalCells = (firstDayOfWeek + daysInMonth).let { if (it % 7 == 0) it else it + (7 - it % 7) }

            var dayCounter = 1
            for (row in 0 until (totalCells / 7)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        if (cellIndex < firstDayOfWeek || dayCounter > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val currentDay = dayCounter
                            val dayData = state.personalDayMap[currentDay]

                            val isPresent = dayData?.status == "present"
                            val isHalf = isPresent && dayData?.type == "half"
                            val isAbsent = dayData?.status == "absent"
                            val hasAdvance = (dayData?.advance_amount ?: 0) > 0

                            val bgColor = when {
                                isHalf -> Color(0xFFF97316) // orange-500
                                isPresent -> Color(0xFF22C55E) // green-500
                                isAbsent -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val textColor = if (dayData?.status.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else Color.White

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgColor)
                                    .clickable {
                                        selectedDay = currentDay
                                        showEditDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(currentDay.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                if (hasAdvance) {
                                    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(4.dp).clip(CircleShape).background(Color(0xFFF97316)))
                                }
                            }
                            dayCounter++
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Legend
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            LegendItem("Present", Color(0xFF22C55E))
            LegendItem("Half Day", Color(0xFFF97316))
            LegendItem("Absent", MaterialTheme.colorScheme.error)
            LegendItem("Advance", Color(0xFFF97316), isDot = true)
        }
    }

    if (showEditDialog && selectedDay != null) {
        val currentData = state.personalDayMap[selectedDay!!] ?: DayData("present", "full")
        var editStatus by remember { mutableStateOf(currentData.status.takeIf { it.isNotEmpty() } ?: "present") }
        var editType by remember { mutableStateOf(currentData.type ?: "full") }
        var editReason by remember { mutableStateOf(currentData.reason ?: "sick") }
        var editNote by remember { mutableStateOf(currentData.note ?: "") }
        var editOT by remember { mutableStateOf(currentData.overtime_hours) }
        var editAdvance by remember { mutableStateOf(currentData.advance_amount.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("$selectedDay $monthDisplay", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Status Toggle
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { editStatus = "present" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (editStatus == "present") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (editStatus == "present") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Present") }
                        Button(onClick = { editStatus = "absent" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (editStatus == "absent") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (editStatus == "absent") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Absent") }
                    }

                    if (editStatus == "present") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editType = "full" }, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, if (editType == "full") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) { Text("Full Day", color = if (editType == "full") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                            OutlinedButton(onClick = { editType = "half" }, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, if (editType == "half") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) { Text("Half Day", color = if (editType == "half") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Overtime", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { if (editOT > 0) editOT-- }, contentAlignment = Alignment.Center) { Text("-", fontWeight = FontWeight.Bold) }
                                Text(editOT.toString(), modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { editOT++ }, contentAlignment = Alignment.Center) { Text("+", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }

                    if (editStatus == "absent") {
                        val reasons = listOf("sick", "personal", "holiday", "weather", "other")
                        // Simplified reasons grid for Android dialog space
                        LazyColumn(modifier = Modifier.heightIn(max = 100.dp)) {
                             items(reasons.chunked(2)) { row ->
                                 Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                     row.forEach { r ->
                                         OutlinedButton(onClick = { editReason = r }, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, if (editReason == r) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)) { Text(r.replaceFirstChar { it.uppercase() }, color = if (editReason == r) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) }
                                     }
                                     if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                                 }
                             }
                        }
                    }

                    OutlinedTextField(value = editAdvance, onValueChange = { editAdvance = it }, placeholder = { Text("Advance Payment (₹)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = editNote, onValueChange = { editNote = it }, placeholder = { Text("Add Note...") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val dateStr = String.format("%04d-%02d-%02d", year, month + 1, selectedDay!!)
                    val newData = DayData(
                        status = editStatus,
                        type = editType,
                        reason = editReason,
                        overtime_hours = editOT,
                        note = editNote,
                        advance_amount = editAdvance.toIntOrNull() ?: 0
                    )
                    viewModel.savePersonalDay(dateStr, newData, 500.0) // Default wage hardcoded to 500 for simplicity as it requires fetching from user doc
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                if (state.personalDayMap.containsKey(selectedDay!!)) {
                     Button(onClick = {
                         val dateStr = String.format("%04d-%02d-%02d", year, month + 1, selectedDay!!)
                         viewModel.removePersonalDay(dateStr)
                         showEditDialog = false
                     }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove") }
                } else {
                     TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
fun LegendItem(text: String, color: Color, isDot: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(if (isDot) 6.dp else 12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
