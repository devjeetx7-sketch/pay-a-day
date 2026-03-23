package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.dailywork.attedance.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreenContent(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    var editName by remember(state.name) { mutableStateOf(state.name) }
    var editWage by remember(state.dailyWage) { mutableStateOf(if (state.dailyWage > 0) state.dailyWage.toInt().toString() else "") }
    var editWorkType by remember(state.workType) { mutableStateOf(state.workType) }
    var customWorkType by remember { mutableStateOf("") }
    var isAddingCustomType by remember { mutableStateOf(false) }
    var showRoleChangeDialog by remember { mutableStateOf(false) }
    var showHowToUseDialog by remember { mutableStateOf(false) }

    val defaultWorkTypes = listOf("Labour", "Helper", "Mistry", "Custom")
    var expandedWorkTypeMenu by remember { mutableStateOf(false) }
    var expandedLanguageMenu by remember { mutableStateOf(false) }
    val supportedLanguages = mapOf("en" to "English", "hi" to "हिंदी", "bn" to "বাংলা", "te" to "తెలుగు", "mr" to "मराठी", "ta" to "தமிழ்", "gu" to "ગુજરાતી")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }

                // Profile Settings
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Profile Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Name
                        Column {
                            Text("Full Name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.saveName(editName) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Text("Save", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Daily Wage
                        Column {
                            Text("Daily Wage", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = editWage,
                                    onValueChange = { editWage = it },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val wageDouble = editWage.toDoubleOrNull()
                                        if (wageDouble != null && wageDouble > 0) {
                                            viewModel.saveWage(wageDouble)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Text("Save", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Work Type
                        Column {
                            Text("Work Role / Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { expandedWorkTypeMenu = true },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (editWorkType.isEmpty()) "Select Work Type" else editWorkType, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expandedWorkTypeMenu,
                                        onDismissRequest = { expandedWorkTypeMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        defaultWorkTypes.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    expandedWorkTypeMenu = false
                                                    if (type == "Custom") {
                                                        isAddingCustomType = true
                                                    } else {
                                                        editWorkType = type
                                                        isAddingCustomType = false
                                                        viewModel.saveWorkType(type)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (isAddingCustomType) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = customWorkType,
                                        onValueChange = { customWorkType = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("e.g. Plumber") },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (customWorkType.trim().isNotEmpty()) {
                                                editWorkType = customWorkType.trim()
                                                viewModel.saveWorkType(editWorkType)
                                                isAddingCustomType = false
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Text("Add", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

                // Role Management
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Role Management", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("App Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Current: ${state.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = { showRoleChangeDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Switch Role", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // General App Settings
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("App Preferences", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(if (state.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Theme", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Switch(checked = state.isDarkMode, onCheckedChange = { viewModel.toggleTheme(it) })
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Reminders", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Daily attendance reminder", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(checked = state.isRemindersEnabled, onCheckedChange = { viewModel.toggleReminders(it) })
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { expandedLanguageMenu = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Blue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Language, contentDescription = null, tint = Color.Blue)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Language", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(supportedLanguages[state.language] ?: "English", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = expandedLanguageMenu,
                                onDismissRequest = { expandedLanguageMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                supportedLanguages.forEach { (code, langName) ->
                                    DropdownMenuItem(
                                        text = { Text(langName, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            expandedLanguageMenu = false
                                            viewModel.saveLanguage(code)
                                        }
                                    )
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFFEAB308).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFEAB308))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("DailyWork Premium", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Upgrade your account", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Data & Export
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Data & Export", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Export as PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Only Contractor Premium", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF25D366).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF25D366))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Share via WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Send digital passbook", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Blue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Blue)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Auto Backup", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Premium feature", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(checked = false, onCheckedChange = { })
                        }
                    }
                }
            }

            // Data & Support
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Data & Support", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().clickable { showHowToUseDialog = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha=0.05f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("How to Use DailyWork", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Quick guide & tutorials", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Data & Privacy Policy", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Stored securely in cloud", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF16A34A).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF16A34A))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Contact Support", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("WhatsApp us for help", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha=0.1f), contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(text = "Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showRoleChangeDialog) {
            AlertDialog(
                onDismissRequest = { showRoleChangeDialog = false },
                title = { Text("Change Role", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to change your role? This will clear your session and take you to the setup screen.") },
                confirmButton = {
                    Button(onClick = { viewModel.changeRole(onRoleCleared = onLogout) }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showRoleChangeDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showHowToUseDialog) {
            AlertDialog(
                onDismissRequest = { showHowToUseDialog = false },
                title = {
                    Column {
                        Text("How to Use DailyWork", fontWeight = FontWeight.Bold)
                        Text("A quick guide to tracking your work effectively", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("Marking Attendance", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Text("On the Dashboard, click Full Day or Half Day to mark today's attendance. Add overtime using the + / - buttons before saving. If you didn't work, click Mark Absent.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text("Net Payable & Earnings", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF16A34A))
                            Text("Your Earnings are automatically calculated by multiplying your working days with your Daily Wage. Net Payable shows your final take-home amount: (Total Earnings - Advance).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text("Advance Payments", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF97316))
                            Text("If you receive money ahead of time, click Add Advance on the Dashboard or add it directly on a specific date inside the Calendar. This is automatically deducted.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text("Calendar & History", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Blue)
                            Text("Use the Calendar to edit past records (e.g. if you forgot to mark attendance yesterday). Use Passbook to export your monthly logs as a PDF or CSV.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showHowToUseDialog = false }) { Text("Got it") }
                }
            )
        }
    }
}
