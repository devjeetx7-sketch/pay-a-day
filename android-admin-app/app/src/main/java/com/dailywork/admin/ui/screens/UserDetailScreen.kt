package com.dailywork.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailywork.admin.data.model.User
import com.dailywork.admin.ui.utils.formatTimestamp
import com.dailywork.admin.viewmodel.UsersViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId: String,
    onBack: () -> Unit,
    onNotifyUser: (String) -> Unit,
    viewModel: UsersViewModel = viewModel()
) {
    val user by viewModel.getUser(userId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        user?.let { u ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Section
                UserHeader(u)

                // Admin Controls Card
                AdminControlsCard(
                    user = u,
                    onNotify = { onNotifyUser(u.uid) },
                    onBlockToggle = { viewModel.toggleBlockStatus(u) },
                    onPremiumToggle = { viewModel.togglePremiumStatus(u) }
                )

                // User Info Card
                UserInfoCard(u)
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun UserHeader(user: User) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(40.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = user.name.take(1).uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user.name.ifEmpty { "Unknown User" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(
                text = if (user.isBlocked) "Blocked" else "Active",
                containerColor = if (user.isBlocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (user.isBlocked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
            )
            StatusChip(
                text = if (user.isPremium) "Premium" else "Free",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun StatusChip(text: String, containerColor: androidx.compose.ui.graphics.Color, contentColor: androidx.compose.ui.graphics.Color) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AdminControlsCard(
    user: User,
    onNotify: () -> Unit,
    onBlockToggle: () -> Unit,
    onPremiumToggle: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Admin Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = onNotify,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Push Notification")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onBlockToggle,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (user.isBlocked) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(if (user.isBlocked) Icons.Default.LockOpen else Icons.Default.Block, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (user.isBlocked) "Unblock" else "Block")
                }

                OutlinedButton(
                    onClick = onPremiumToggle,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (user.isPremium) Icons.Default.StarOutline else Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (user.isPremium) "Set Free" else "Set Premium")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            LiveStatusIndicator(user.lastActive)
        }
    }
}

@Composable
fun LiveStatusIndicator(lastActive: Long) {
    val isOnline = System.currentTimeMillis() - lastActive < 5 * 60 * 1000 // 5 minutes threshold
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = RoundedCornerShape(5.dp),
            color = if (isOnline) androidx.compose.ui.graphics.Color.Green else androidx.compose.ui.graphics.Color.Gray
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isOnline) "Online" else "Offline • Last seen ${formatTimestamp(lastActive)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UserInfoCard(user: User) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Account Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            InfoRow(icon = Icons.Default.Badge, label = "User ID", value = user.uid)
            InfoRow(icon = Icons.Default.Person, label = "Role", value = user.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
            InfoRow(icon = Icons.Default.CalendarToday, label = "Joined Date", value = formatTimestamp(user.createdAt))
        }
    }
}


@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
