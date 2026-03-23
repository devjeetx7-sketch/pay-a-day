package com.dailywork.attedance.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LanguageOption(val code: String, val title: String, val iconText: String)

val languages = listOf(
    LanguageOption("en", "English", "A"),
    LanguageOption("hi", "hindi", "a"),
    LanguageOption("mr", "marathi", "m"),
    LanguageOption("gu", "gujarati", "g")
)

data class RoleOption(val id: String, val title: String, val desc: String, val icon: ImageVector, val color: Color)

val roles = listOf(
    RoleOption("labour", "Labour", "Daily wage worker", Icons.Default.Construction, Color(0xFF3B82F6)), // blue-500
    RoleOption("helper", "Helper", "Assists mistris", Icons.Default.Handshake, Color(0xFF10B981)), // green-500
    RoleOption("mistry", "Mistry", "Skilled mason", Icons.Default.PersonOutline, Color(0xFFA855F7)), // purple-500
    RoleOption("contractor", "Contractor", "Manages multiple workers", Icons.Default.Business, Color(0xFFF97316)) // orange-500
)

@Composable
fun RoleSelectionScreen(authViewModel: AuthViewModel, onComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    var selectedLang by remember { mutableStateOf<String?>(null) }
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = step == 1,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + slideInVertically(initialOffsetY = { 50 }),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) + slideOutVertically(targetOffsetY = { -50 })
        ) {
            LanguageStep(
                selectedLang = selectedLang,
                onLangSelect = { selectedLang = it },
                onNext = {
                    coroutineScope.launch {
                        delay(200)
                        step = 2
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = step == 2,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + slideInVertically(initialOffsetY = { 50 }),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) + slideOutVertically(targetOffsetY = { -50 })
        ) {
            RoleStep(
                selectedRole = selectedRole,
                onRoleSelect = { selectedRole = it },
                onSave = {
                    saving = true
                    coroutineScope.launch {
                        authViewModel.savePreferences(selectedRole!!, selectedLang ?: "en")
                        onComplete()
                    }
                },
                saving = saving
            )
        }
    }
}

@Composable
fun LanguageStep(selectedLang: String?, onLangSelect: (String) -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select Language", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Text("Choose your preferred language", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        // Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LanguageCard(languages[0], selectedLang == languages[0].code, { onLangSelect(languages[0].code) }, Modifier.weight(1f))
                LanguageCard(languages[1], selectedLang == languages[1].code, { onLangSelect(languages[1].code) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LanguageCard(languages[2], selectedLang == languages[2].code, { onLangSelect(languages[2].code) }, Modifier.weight(1f))
                LanguageCard(languages[3], selectedLang == languages[3].code, { onLangSelect(languages[3].code) }, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            enabled = selectedLang != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pressScaleEffect(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun LanguageCard(option: LanguageOption, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
            .border(
                2.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(option.iconText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(option.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun RoleStep(selectedRole: String?, onRoleSelect: (String) -> Unit, onSave: () -> Unit, saving: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Role", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Text("How will you use this app?", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            roles.forEach { role ->
                RoleCard(role, selectedRole == role.id) { onRoleSelect(role.id) }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSave,
            enabled = selectedRole != null && !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pressScaleEffect(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (saving) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RoleCard(role: RoleOption, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
            .border(
                2.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(role.icon, contentDescription = null, tint = role.color, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(role.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isSelected) role.color else MaterialTheme.colorScheme.onSurface)
            Text(role.desc, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
