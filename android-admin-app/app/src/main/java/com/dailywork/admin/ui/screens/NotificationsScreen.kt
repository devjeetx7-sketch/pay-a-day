package com.dailywork.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailywork.admin.viewmodel.NotificationsViewModel

@Composable
fun NotificationsScreen(
    targetUserId: String? = null,
    viewModel: NotificationsViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf(if (targetUserId != null) "Single User" else "All Users") }
    val targets = if (targetUserId != null) listOf("Single User") else listOf("All Users", "Contractors", "Personal Users")

    val isSending by viewModel.isSending.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Send Notification", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Text(text = "Select Target Group", fontSize = 14.sp, fontWeight = FontWeight.Medium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            targets.forEach { target ->
                FilterChip(
                    selected = selectedTarget == target,
                    onClick = { selectedTarget = target },
                    label = { Text(target) }
                )
            }
        }

        if (targetUserId != null) {
            Text(text = "Targeting UID: $targetUserId", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Notification Title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Notification Message") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val target = if (selectedTarget == "Single User") "User: $targetUserId" else selectedTarget
                viewModel.sendNotification(target, title, message)
                title = ""
                message = ""
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = title.isNotEmpty() && message.isNotEmpty() && !isSending
        ) {
            if (isSending) {
                CircularProgressIndicator(size = 24.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Send Push Notification")
            }
        }
    }
}
