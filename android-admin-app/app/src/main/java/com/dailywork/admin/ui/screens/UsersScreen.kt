package com.dailywork.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailywork.admin.data.model.User
import com.dailywork.admin.viewmodel.UsersViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications

@Composable
fun UsersScreen(
    onNotifyUser: (String) -> Unit,
    viewModel: UsersViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Contractors", "Personal")

    val contractors by viewModel.contractors.collectAsState()
    val personalUsers by viewModel.personalUsers.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        val currentList = if (selectedTab == 0) contractors else personalUsers

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(currentList) { user ->
                UserCard(
                    user,
                    onBlockToggle = { viewModel.toggleBlockStatus(user) },
                    onNotify = { onNotifyUser(user.uid) }
                )
            }
        }
    }
}

@Composable
fun UserCard(user: User, onBlockToggle: () -> Unit, onNotify: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = user.name.ifEmpty { "Unknown User" }, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = user.email, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (user.isPremium) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("PREMIUM") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNotify) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notify User", tint = MaterialTheme.colorScheme.primary)
                }

                Column {
                    Text(
                        text = "Status: ${if (user.isBlocked) "Blocked" else "Active"}",
                        fontSize = 14.sp,
                        color = if (user.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Last Active: ${formatTimestamp(user.lastActive)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = user.isBlocked,
                    onCheckedChange = { onBlockToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.error,
                        checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                    )
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
