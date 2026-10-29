package com.desloth.app.ui.components

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desloth.app.service.DeSlothInterceptorService
import com.desloth.app.ui.theme.AccentAmber
import com.desloth.app.ui.theme.AccentEmerald
import com.desloth.app.ui.theme.BorderStroke
import com.desloth.app.ui.theme.DarkSurface
import com.desloth.app.ui.theme.PrimarySky
import com.desloth.app.ui.theme.TextMuted
import com.desloth.app.ui.theme.TextPrimary
import com.desloth.app.ui.theme.TextSecondary

data class SystemPermissionsState(
    val hasUsageAccess: Boolean,
    val hasOverlayAccess: Boolean,
    val hasAccessibilityAccess: Boolean
) {
    val allGranted: Boolean
        get() = hasUsageAccess && hasOverlayAccess && hasAccessibilityAccess
}

object PermissionUtils {

    fun checkPermissions(context: Context): SystemPermissionsState {
        return SystemPermissionsState(
            hasUsageAccess = checkUsageAccess(context),
            hasOverlayAccess = Settings.canDrawOverlays(context),
            hasAccessibilityAccess = checkAccessibilityAccess(context)
        )
    }

    private fun checkUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun checkAccessibilityAccess(context: Context): Boolean {
        val expectedService = ComponentName(context, DeSlothInterceptorService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val component = splitter.next()
            if (component.equals(expectedService, ignoreCase = true) ||
                component.contains(context.packageName, ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }
}

@Composable
fun PermissionStatusBanner(
    permissionsState: SystemPermissionsState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (permissionsState.allGranted) BorderStroke else AccentAmber, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (permissionsState.allGranted) AccentEmerald else AccentAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYSTEM ENGINE GUARDS",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (permissionsState.allGranted) AccentEmerald else AccentAmber,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (permissionsState.allGranted) "ALL ACTIVE" else "CONFIGURATION REQUIRED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (permissionsState.allGranted) AccentEmerald else AccentAmber
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Usage Access Item
            PermissionRowItem(
                title = "Usage Access",
                description = "Enables foreground minute tracking for blacklisted apps",
                isGranted = permissionsState.hasUsageAccess,
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Overlay Permission Item
            PermissionRowItem(
                title = "System Overlay Window",
                description = "Required to render the Friction and Circuit Breaker gates",
                isGranted = permissionsState.hasOverlayAccess,
                onGrantClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Accessibility Service Item
            PermissionRowItem(
                title = "DeSloth Interceptor Service",
                description = "Intercepts foreground package transitions without network access",
                isGranted = permissionsState.hasAccessibilityAccess,
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun PermissionRowItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isGranted) AccentEmerald else AccentAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (!isGranted) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onGrantClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimarySky
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
