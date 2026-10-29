package com.desloth.app.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desloth.app.data.datastore.BypassState
import com.desloth.app.data.repository.LedgerStatus
import com.desloth.app.data.repository.StimulationLedgerState
import com.desloth.app.ui.theme.AccentAmber
import com.desloth.app.ui.theme.AccentCrimson
import com.desloth.app.ui.theme.AccentEmerald
import com.desloth.app.ui.theme.DarkSurface
import com.desloth.app.ui.theme.DarkSurfaceVariant
import com.desloth.app.ui.theme.PrimarySky
import com.desloth.app.ui.theme.TextMuted
import com.desloth.app.ui.theme.TextPrimary
import com.desloth.app.ui.theme.TextSecondary

@Composable
fun LedgerHeaderCard(
    state: StimulationLedgerState,
    bypassState: BypassState?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (state.status) {
        LedgerStatus.SURPLUS -> AccentEmerald
        LedgerStatus.FRICTION -> AccentAmber
        LedgerStatus.CIRCUIT_BREAKER -> AccentCrimson
    }

    val statusLabel = when (state.status) {
        LedgerStatus.SURPLUS -> "SURPLUS (ACCESS PERMITTED)"
        LedgerStatus.FRICTION -> "FRICTION GATE ACTIVE (-49..0)"
        LedgerStatus.CIRCUIT_BREAKER -> "CIRCUIT BREAKER ENGAGED (≤ -50)"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, statusColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Header Top Row: Status badge and Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recalculate Ledger",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Stimulation Balance B
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (state.balance > 0) "+${state.balance}" else "${state.balance}",
                    style = MaterialTheme.typography.displayLarge,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "UNITS",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Text(
                text = "Live balance = EarnedUnits - (3 × Tier1_Min + 1 × Tier2_Min)",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Metrics Grid Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    title = "Earned Today",
                    value = "+${state.earnedUnits}",
                    subtitle = "Completed tasks",
                    valueColor = AccentEmerald,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                MetricTile(
                    title = "Tier 1 Burn",
                    value = "${state.tier1Minutes}m",
                    subtitle = "3x = -${state.tier1Minutes * 3}",
                    valueColor = AccentCrimson,
                    icon = Icons.Default.TrendingDown,
                    modifier = Modifier.weight(1f)
                )

                MetricTile(
                    title = "Tier 2 Burn",
                    value = "${state.tier2Minutes}m",
                    subtitle = "1x = -${state.tier2Minutes}",
                    valueColor = AccentAmber,
                    icon = Icons.Default.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
            }

            // Active Bypass Indicator (if any 10-minute timer running)
            AnimatedVisibility(visible = bypassState?.isActive == true) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .border(1.dp, PrimarySky.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = PrimarySky,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bypass Active: ${bypassState?.packageName} (${bypassState?.remainingSeconds ?: 0}s left)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimarySky,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}
