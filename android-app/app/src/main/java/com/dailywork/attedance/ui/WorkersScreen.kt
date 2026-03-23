package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailywork.attedance.viewmodel.WorkerItem
import com.dailywork.attedance.viewmodel.WorkersViewModel
import java.util.Locale

@Composable
fun WorkersScreenContent(
    viewModel: WorkersViewModel,
    navController: NavController
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFormDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var editingWorker by remember { mutableStateOf<WorkerItem?>(null) }

    val filteredWorkers = state.workers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) || it.workType.contains(searchQuery, ignoreCase = true)
    }

    if (state.isLoading && state.role.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Manage Workers", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, phone or role...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredWorkers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (state.workers.isEmpty()) "No workers found" else "No matching workers", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.workers.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { editingWorker = null; showFormDialog = true }, shape = RoundedCornerShape(12.dp)) {
                                Text("Add Worker")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredWorkers) { worker ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { navController.navigate("worker_detail/${worker.id}") },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                            Text(worker.name.take(1).uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(worker.workType, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { navController.navigate("worker_detail/${worker.id}") }) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "View Details")
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Daily Wage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                        Text("₹${worker.wage.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }
                                    if (worker.phone.isNotEmpty()) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Phone", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                            Text(worker.phone, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { editingWorker = worker; showFormDialog = true },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showDeleteDialog = worker.id },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { editingWorker = null; showFormDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 64.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Worker")
        }

        if (showFormDialog) {
            WorkerFormDialog(
                worker = editingWorker,
                onDismiss = { showFormDialog = false; editingWorker = null },
                onSave = {
                    viewModel.saveWorker(it)
                    showFormDialog = false
                    editingWorker = null
                },
                isSaving = state.isSaving
            )
        }

        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Worker", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to permanently delete this worker? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteWorker(showDeleteDialog!!); showDeleteDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun WorkerFormDialog(
    worker: WorkerItem?,
    onDismiss: () -> Unit,
    onSave: (WorkerItem) -> Unit,
    isSaving: Boolean
) {
    var name by remember { mutableStateOf(worker?.name ?: "") }
    var phone by remember { mutableStateOf(worker?.phone ?: "") }
    var aadhar by remember { mutableStateOf(worker?.aadhar ?: "") }
    var age by remember { mutableStateOf(worker?.age ?: "") }
    var workType by remember { mutableStateOf(worker?.workType ?: "Labour") }
    var wage by remember { mutableStateOf(worker?.wage?.toInt()?.toString() ?: "500") }

    val defaultWorkTypes = listOf("Labour", "Helper", "Mistry", "Custom")
    var expandedMenu by remember { mutableStateOf(false) }
    var isCustomType by remember { mutableStateOf(false) }
    var customTypeStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (worker == null) "Add New Worker" else "Edit Worker", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wage,
                        onValueChange = { wage = it },
                        label = { Text("Daily Wage *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Text("₹") }
                    )
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedMenu = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isCustomType) "Custom Type" else workType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        defaultWorkTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    expandedMenu = false
                                    if (type == "Custom") {
                                        isCustomType = true
                                    } else {
                                        workType = type
                                        isCustomType = false
                                    }
                                }
                            )
                        }
                    }
                }

                if (isCustomType) {
                    OutlinedTextField(
                        value = customTypeStr,
                        onValueChange = { customTypeStr = it },
                        label = { Text("Custom Work Type *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = aadhar,
                    onValueChange = { aadhar = it },
                    label = { Text("Aadhar Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalWorkType = if (isCustomType) customTypeStr else workType
                    val finalWage = wage.toDoubleOrNull() ?: 500.0
                    if (name.isNotBlank() && finalWorkType.isNotBlank() && finalWage > 0) {
                        onSave(
                            WorkerItem(
                                id = worker?.id ?: "",
                                name = name.trim(),
                                phone = phone.trim(),
                                aadhar = aadhar.trim(),
                                age = age.trim(),
                                workType = finalWorkType.trim(),
                                wage = finalWage,
                                contractorId = ""
                            )
                        )
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                else Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        }
    )
}
