package com.dailywork.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailywork.admin.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(text = "Global Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Feature Flags", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                ToggleItem(
                    title = "Language Selection",
                    description = "Enable/Disable language choice in user app",
                    checked = config.languageEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(languageEnabled = it)) }
                )

                HorizontalDivider()

                ToggleItem(
                    title = "Push Notifications",
                    description = "Global toggle for push notifications",
                    checked = config.notificationsEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(notificationsEnabled = it)) }
                )

                HorizontalDivider()

                ToggleItem(
                    title = "Role UI Visibility",
                    description = "Show/Hide role selection in user app",
                    checked = config.roleUiEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(roleUiEnabled = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout Admin Session")
        }
    }
}

@Composable
fun ToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
