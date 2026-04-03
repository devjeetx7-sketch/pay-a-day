package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class HistoryRecord(
    val id: String,
    val workerName: String,
    val date: String,
    val type: String, // "Attendance", "Payment"
    val description: String,
    val amount: Double? = null,
    val status: String? = null // "Present", "Absent", "Half Day"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerHistoryScreen(navController: NavController) {
    var expandedFilterMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    val mockData = listOf(
        HistoryRecord("1", "Raju", "12 Oct 2023", "Attendance", "Daily Work", status = "Present"),
        HistoryRecord("2", "Raju", "12 Oct 2023", "Payment", "Advance Payment", amount = 500.0),
        HistoryRecord("3", "Suresh", "11 Oct 2023", "Attendance", "Daily Work", status = "Half Day"),
        HistoryRecord("4", "Raju", "10 Oct 2023", "Attendance", "Daily Work", status = "Absent"),
        HistoryRecord("5", "Suresh", "09 Oct 2023", "Payment", "Weekly Settlement", amount = 3000.0)
    )

    val filteredData = if (selectedFilter == "All") {
        mockData
    } else {
        mockData.filter { it.type == selectedFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Worker History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Filters Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Filter:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box {
                    OutlinedButton(
                        onClick = { expandedFilterMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(selectedFilter, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expandedFilterMenu,
                        onDismissRequest = { expandedFilterMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        listOf("All", "Attendance", "Payment").forEach { filterOption ->
                            DropdownMenuItem(
                                text = { Text(filterOption, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedFilter = filterOption
                                    expandedFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline)

            // History List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredData) { record ->
                    HistoryItemCard(record)
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(record: HistoryRecord) {
    val isPayment = record.type == "Payment"
    val iconColor = if (isPayment) Color(0xFFF97316) else MaterialTheme.colorScheme.primary
    val icon = if (isPayment) Icons.Default.Payment else Icons.Default.Work

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(record.workerName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("${record.date} • ${record.type}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (isPayment) {
                Text("₹${record.amount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = iconColor)
                Text(record.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val statusColor = when (record.status) {
                    "Present" -> Color(0xFF10B981)
                    "Half Day" -> Color(0xFFF59E0B)
                    "Absent" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(record.status ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
        }
    }
}
