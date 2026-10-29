package com.desloth.app.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desloth.app.data.local.entities.TargetAppEntity
import com.desloth.app.ui.theme.AccentAmber
import com.desloth.app.ui.theme.AccentCrimson
import com.desloth.app.ui.theme.BorderStroke
import com.desloth.app.ui.theme.DarkSurface
import com.desloth.app.ui.theme.DarkSurfaceVariant
import com.desloth.app.ui.theme.PrimarySky
import com.desloth.app.ui.theme.TextMuted
import com.desloth.app.ui.theme.TextPrimary
import com.desloth.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val appName: String,
    val packageName: String
)

@Composable
fun TargetAppsSection(
    targetApps: List<TargetAppEntity>,
    usageMap: Map<String, Long>,
    onAddTargetApp: (packageName: String, appName: String, tier: Int) -> Unit,
    onUpdateTier: (packageName: String, newTier: Int) -> Unit,
    onDeleteTargetApp: (packageName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPickerModal by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BLACKLISTED TARGET APPS",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tier 1: 3x drain/min | Tier 2: 1x drain/min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            OutlinedButton(
                onClick = { showPickerModal = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimarySky),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Select App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (targetApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No blacklisted apps configured. Select an installed app or seed sample targets.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                targetApps.forEach { app ->
                    val minutesUsed = usageMap[app.packageName] ?: 0L
                    TargetAppRowItem(
                        app = app,
                        minutesToday = minutesUsed,
                        onToggleTier = { newTier -> onUpdateTier(app.packageName, newTier) },
                        onDelete = { onDeleteTargetApp(app.packageName) }
                    )
                }
            }
        }
    }

    if (showPickerModal) {
        InstalledPackagePickerDialog(
            alreadyConfiguredPackages = targetApps.map { it.packageName }.toSet(),
            onDismiss = { showPickerModal = false },
            onAppSelected = { pkg, name, tier ->
                onAddTargetApp(pkg, name, tier)
                showPickerModal = false
            }
        )
    }
}

@Composable
private fun TargetAppRowItem(
    app: TargetAppEntity,
    minutesToday: Long,
    onToggleTier: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val tierColor = if (app.tier == 1) AccentCrimson else AccentAmber
    val drainRate = app.drainRatePerMinute
    val totalPenalty = minutesToday * drainRate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(10.dp))
            .border(1.dp, BorderStroke, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(tierColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, tierColor, RoundedCornerShape(4.dp))
                        .clickable {
                            val nextTier = if (app.tier == 1) 2 else 1
                            onToggleTier(nextTier)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TIER ${app.tier} (${drainRate}x DRAIN)",
                        color = tierColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Today: $minutesToday min foreground (-$totalPenalty units drained)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (minutesToday > 0) tierColor else TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove App",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun InstalledPackagePickerDialog(
    alreadyConfiguredPackages: Set<String>,
    onDismiss: () -> Unit,
    onAppSelected: (packageName: String, appName: String, tier: Int) -> Unit
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTier by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val appList = resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) null
                else {
                    val label = info.loadLabel(pm).toString()
                    InstalledAppItem(appName = label, packageName = pkg)
                }
            }
                .distinctBy { it.packageName }
                .sortedBy { it.appName }

            installedApps = appList
            isLoading = false
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Assign Target Blacklist App", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.height(400.dp)) {
                // Tier Selection
                Text("Select Penalty Tier:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { selectedTier = 1 }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedTier == 1,
                            onClick = { selectedTier = 1 },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentCrimson)
                        )
                        Text("Tier 1 (3x Drain)", color = TextPrimary, fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { selectedTier = 2 }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedTier == 2,
                            onClick = { selectedTier = 2 },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentAmber)
                        )
                        Text("Tier 2 (1x Drain)", color = TextPrimary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed applications...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimarySky,
                        unfocusedBorderColor = BorderStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Loading installed packages...", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps) { item ->
                            val isAlreadyTarget = alreadyConfiguredPackages.contains(item.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isAlreadyTarget) Color(0xFF1E293B) else DarkSurfaceVariant,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable(enabled = !isAlreadyTarget) {
                                        onAppSelected(item.packageName, item.appName, selectedTier)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.appName,
                                        color = if (isAlreadyTarget) TextMuted else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = item.packageName,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (isAlreadyTarget) {
                                    Text("Added", color = TextMuted, fontSize = 10.sp)
                                } else {
                                    Text("+ Select", color = PrimarySky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
