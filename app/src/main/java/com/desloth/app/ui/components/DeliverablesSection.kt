package com.desloth.app.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desloth.app.data.local.entities.DeliverableEntity
import com.desloth.app.ui.theme.AccentEmerald
import com.desloth.app.ui.theme.BorderStroke
import com.desloth.app.ui.theme.DarkSurface
import com.desloth.app.ui.theme.DarkSurfaceVariant
import com.desloth.app.ui.theme.PrimarySky
import com.desloth.app.ui.theme.SecondaryIndigo
import com.desloth.app.ui.theme.TextMuted
import com.desloth.app.ui.theme.TextPrimary
import com.desloth.app.ui.theme.TextSecondary

@Composable
fun DeliverablesSection(
    deliverables: List<DeliverableEntity>,
    onToggleComplete: (id: Long, completed: Boolean) -> Unit,
    onDelete: (id: Long) -> Unit,
    onAddDeliverable: (title: String, unitValue: Int, isMicroTask: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DAILY DELIVERABLES & MICRO-TASKS",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Complete items to earn stimulation ledger balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            OutlinedButton(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimarySky),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (deliverables.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No deliverables added yet. Tap '+ Add Task' or seed sample data below.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                deliverables.forEach { item ->
                    DeliverableItemRow(
                        item = item,
                        onToggle = { completed -> onToggleComplete(item.id, completed) },
                        onDelete = { onDelete(item.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddDeliverableDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, units, isMicro ->
                onAddDeliverable(title, units, isMicro)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun DeliverableItemRow(
    item: DeliverableEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = item.completedToday

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (isCompleted) AccentEmerald.copy(alpha = 0.5f) else BorderStroke,
                RoundedCornerShape(10.dp)
            )
            .clickable { onToggle(!isCompleted) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isCompleted,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = AccentEmerald,
                uncheckedColor = TextSecondary,
                checkmarkColor = Color.White
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isMicroTask) {
                    Box(
                        modifier = Modifier
                            .background(SecondaryIndigo.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, SecondaryIndigo, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = SecondaryIndigo,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "5-MIN MICRO",
                                color = SecondaryIndigo,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = "+${item.unitValue} UNITS",
                    color = if (isCompleted) AccentEmerald else PrimarySky,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCompleted) TextMuted else TextPrimary,
                fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Task",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AddDeliverableDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, unitValue: Int, isMicroTask: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var unitValueStr by remember { mutableStateOf("30") }
    var isMicroTask by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Daily Deliverable / Task", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g. Complete systems architecture design") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimarySky,
                        unfocusedBorderColor = BorderStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = unitValueStr,
                    onValueChange = { unitValueStr = it },
                    label = { Text("Stimulation Units Awarded") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimarySky,
                        unfocusedBorderColor = BorderStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("5-Minute Micro-Task", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Can be deployed by Circuit Breaker gate", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isMicroTask,
                        onCheckedChange = {
                            isMicroTask = it
                            if (it && unitValueStr == "30") {
                                unitValueStr = "15"
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimarySky,
                            checkedTrackColor = SecondaryIndigo.copy(alpha = 0.5f)
                        )
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val units = unitValueStr.toIntOrNull()
                    if (title.isBlank()) {
                        errorMessage = "Title cannot be blank"
                        return@Button
                    }
                    if (units == null || units <= 0) {
                        errorMessage = "Please enter valid positive units"
                        return@Button
                    }
                    onConfirm(title.trim(), units, isMicroTask)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySky)
            ) {
                Text("Add Task", color = DarkSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
