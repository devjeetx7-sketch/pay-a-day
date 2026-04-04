package com.dailywork.attedance.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import coil.compose.AsyncImage
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.dailywork.attedance.ui.MainActivity
import com.dailywork.attedance.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onNavigateToPremium: () -> Unit = {},
    onNavigateToWorkerHistory: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.triggerRestart) {
        if (state.triggerRestart) {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            // No need for Runtime.exit(0), FLAG_ACTIVITY_CLEAR_TASK handles it
        }
    }

    var showHowToUseBottomSheet by remember { mutableStateOf(false) }
    var showPrivacyBottomSheet by remember { mutableStateOf(false) }
    var showDeveloperBottomSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var expandedLanguageMenu by remember { mutableStateOf(false) }
    var showRoleBottomSheet by remember { mutableStateOf(false) }
    var showRoleConfirmationDialog by remember { mutableStateOf(false) }
    var pendingRole by remember { mutableStateOf("") }
    val supportedLanguages = mapOf("en" to "English", "hi" to "हिंदी", "bn" to "বাংলা", "te" to "తెలుగు", "mr" to "मराठी", "ta" to "தமிழ்", "gu" to "ગુજરાતી")

    val hasChanges = state.name != state.originalName ||
            state.dailyWage != state.originalWage ||
            state.role != state.originalRole ||
            state.phone != state.originalPhone ||
            state.profileImageUri != null

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Profile Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                        AnimatedVisibility(visible = hasChanges) {
                            IconButton(onClick = { viewModel.saveChanges() }, enabled = !state.isSaving) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = "Save Changes", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                        // Profile Image (Role-Based Icon)
                        val personalIcons = listOf(Icons.Default.Construction, Icons.Default.FormatPaint, Icons.Default.Build, Icons.Default.Plumbing)
                        val randomPersonalIcon = remember(state.role) { personalIcons.random() }

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.role == "contractor") {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
                            } else {
                                Icon(randomPersonalIcon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Name
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { viewModel.onNameChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Email
                        OutlinedTextField(
                            value = FirebaseAuth.getInstance().currentUser?.email ?: "",
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Daily Wage
                        OutlinedTextField(
                            value = if (state.dailyWage > 0) state.dailyWage.toInt().toString() else "",
                            onValueChange = { viewModel.onWageChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Daily Wage") },
                            leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Phone Number
                        OutlinedTextField(
                            value = state.phone,
                            onValueChange = { newValue ->
                                val digits = newValue.filter { it.isDigit() }.take(10)
                                viewModel.onPhoneChange(digits)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Phone Number") },
                            leadingIcon = { Text("+91 ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
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
                        Row(modifier = Modifier.fillMaxWidth().clickable { showRoleBottomSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Role Management", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Current: ${state.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().clickable { if (!state.isPremium) onNavigateToPremium() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (state.isPremium) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEAB308).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    if (state.isPremium) {
                                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF10B981))
                                    } else {
                                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFEAB308))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    if (state.isPremium) {
                                        Text("DailyWork Premium", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        Text("Thank you for your purchase ❤️", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text("Upgrade to Premium", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("Unlock all features", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            if (!state.isPremium) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Data & Export
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Data & Export", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (state.role == "contractor") {
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToWorkerHistory() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Worker History", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("Attendance & Payment logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Divider(color = MaterialTheme.colorScheme.outline)
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
                        }

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
                    }
                }
            }

            // Data & Support
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Data & Support", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().clickable { showHowToUseBottomSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                        Row(modifier = Modifier.fillMaxWidth().clickable { showPrivacyBottomSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                        Row(modifier = Modifier.fillMaxWidth().clickable { showDeveloperBottomSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF16A34A).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF16A34A))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Developer Contact", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Reach out for feedback", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha=0.1f), contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log Out", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to log out of your account?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Log Out", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showRoleConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showRoleConfirmationDialog = false },
                title = { Text("Change Role", fontWeight = FontWeight.Bold) },
                text = { Text("Changing role will refresh the app and switch your dashboard.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showRoleConfirmationDialog = false
                            viewModel.onRoleChange(pendingRole)
                            viewModel.saveChanges()
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRoleConfirmationDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showHowToUseBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showHowToUseBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("How to Use DailyWork", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("A quick guide to tracking your work effectively", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Divider(color = MaterialTheme.colorScheme.outline)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Marking Attendance", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("On the Dashboard, click Full Day or Half Day to mark today's attendance. Add overtime using the + / - buttons before saving. If you didn't work, click Mark Absent.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("2. Net Payable & Earnings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Your Earnings are automatically calculated by multiplying your working days with your Daily Wage. Net Payable shows your final take-home amount: (Total Earnings - Advance).", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("3. Advance Payments", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("If you receive money ahead of time, click Add Advance on the Dashboard or add it directly on a specific date inside the Calendar. This is automatically deducted.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("4. Calendar & History", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Use the Calendar to edit past records (e.g. if you forgot to mark attendance yesterday). Use Passbook to export your monthly logs as a PDF or CSV.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (showPrivacyBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPrivacyBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Data & Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Stored securely in the cloud", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Divider(color = MaterialTheme.colorScheme.outline)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Data Collection", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("We collect your name, phone number, and attendance records to provide the core functionality of the app.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Storage Method", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Your data is securely stored on Firebase, protected by industry-standard security measures.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Security Practices", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Access to your data is restricted to your authenticated account.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("User Control", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("You can delete your data by deleting your account, or export your records from the app.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (showDeveloperBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDeveloperBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Developer Contact", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Reach out to us for any feedback or support", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Divider(color = MaterialTheme.colorScheme.outline)

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("support@dailywork.com", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("+91 98765 43210", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("www.dailywork.com", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (showRoleBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRoleBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Role Management", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Switch between Contractor and Personal mode", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Divider(color = MaterialTheme.colorScheme.outline)

                    listOf("contractor", "personal").forEach { roleOption ->
                        val isSelected = state.role == roleOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!isSelected) {
                                        pendingRole = roleOption
                                        showRoleConfirmationDialog = true
                                    }
                                    showRoleBottomSheet = false
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = roleOption.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
