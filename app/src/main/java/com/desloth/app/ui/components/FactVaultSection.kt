package com.desloth.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desloth.app.data.local.entities.FactVaultEntity
import com.desloth.app.ui.theme.AccentAmber
import com.desloth.app.ui.theme.BorderStroke
import com.desloth.app.ui.theme.DarkSurface
import com.desloth.app.ui.theme.PrimarySky
import com.desloth.app.ui.theme.TextMuted
import com.desloth.app.ui.theme.TextPrimary
import com.desloth.app.ui.theme.TextSecondary
import java.util.concurrent.TimeUnit

@Composable
fun FactVaultSection(
    facts: List<FactVaultEntity>,
    onAddFact: (statement: String, goalTitle: String, deadlineMillis: Long, requiredDailyHours: Double) -> Unit,
    onDeleteFact: (id: Long) -> Unit,
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
                    text = "FACT VAULT COGNITIVE ANCHORS",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verbatim anchors typed to unlock friction gates (-49..0)",
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
                Text("Add Anchor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (facts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No cognitive anchors registered in Fact Vault. Add one or seed sample anchors.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                facts.forEach { fact ->
                    FactVaultRowItem(
                        fact = fact,
                        onDelete = { onDeleteFact(fact.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddFactDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { statement, goal, deadlineMillis, hours ->
                onAddFact(statement, goal, deadlineMillis, hours)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun FactVaultRowItem(
    fact: FactVaultEntity,
    onDelete: () -> Unit
) {
    val daysLeft = fact.daysRemaining()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(10.dp))
            .border(1.dp, BorderStroke, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LockClock,
                    contentDescription = null,
                    tint = PrimarySky,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = fact.goalTitle.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimarySky,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Anchor",
                    tint = TextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "⏳ $daysLeft days remaining | ${fact.requiredDailyHours}h daily requirement",
            style = MaterialTheme.typography.bodyMedium,
            color = AccentAmber,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                .padding(10.dp)
        ) {
            Text(
                text = "\"${fact.statement}\"",
                color = TextPrimary,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun AddFactDialog(
    onDismiss: () -> Unit,
    onConfirm: (statement: String, goalTitle: String, deadlineMillis: Long, requiredDailyHours: Double) -> Unit
) {
    var goalTitle by remember { mutableStateOf("") }
    var statement by remember { mutableStateOf("") }
    var daysLeftStr by remember { mutableStateOf("30") }
    var dailyHoursStr by remember { mutableStateOf("4.0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Fact Vault Anchor", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = goalTitle,
                    onValueChange = { goalTitle = it },
                    label = { Text("Core Goal / Milestone") },
                    placeholder = { Text("e.g. Android Core Architecture Release") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimarySky,
                        unfocusedBorderColor = BorderStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = statement,
                    onValueChange = { statement = it },
                    label = { Text("Verbatim Anchor Statement") },
                    placeholder = { Text("Type statement exactly required during friction...") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimarySky,
                        unfocusedBorderColor = BorderStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = daysLeftStr,
                        onValueChange = { daysLeftStr = it },
                        label = { Text("Days to Deadline") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimarySky,
                            unfocusedBorderColor = BorderStroke
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = dailyHoursStr,
                        onValueChange = { dailyHoursStr = it },
                        label = { Text("Daily Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimarySky,
                            unfocusedBorderColor = BorderStroke
                        ),
                        modifier = Modifier.weight(1f)
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
                    val days = daysLeftStr.toLongOrNull()
                    val hours = dailyHoursStr.toDoubleOrNull()
                    if (goalTitle.isBlank()) {
                        errorMessage = "Goal title cannot be blank"
                        return@Button
                    }
                    if (statement.isBlank()) {
                        errorMessage = "Statement cannot be blank"
                        return@Button
                    }
                    if (days == null || days <= 0) {
                        errorMessage = "Please enter valid days to deadline"
                        return@Button
                    }
                    if (hours == null || hours <= 0) {
                        errorMessage = "Please enter valid daily hours"
                        return@Button
                    }
                    val deadlineMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)
                    onConfirm(statement.trim(), goalTitle.trim(), deadlineMillis, hours)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySky)
            ) {
                Text("Register Anchor", color = DarkSurface)
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
