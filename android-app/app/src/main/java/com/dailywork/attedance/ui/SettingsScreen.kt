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
import androidx.compose.foundation.border
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

    val defaultWorkTypes = listOf("Labour", "Helper", "Mistry", "Custom")
    var expandedWorkTypeMenu by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                Text("General Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Blue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = Color.Blue)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Language", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("English", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // Data & Support
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Data & Support", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
    }
}
