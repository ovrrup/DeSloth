package com.desloth.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desloth.app.DeSlothApp
import com.desloth.app.ui.components.DeliverablesSection
import com.desloth.app.ui.components.FactVaultSection
import com.desloth.app.ui.components.LedgerHeaderCard
import com.desloth.app.ui.components.PermissionStatusBanner
import com.desloth.app.ui.components.SeedDataCard
import com.desloth.app.ui.components.TargetAppsSection
import com.desloth.app.ui.theme.DarkBackground
import com.desloth.app.ui.theme.DarkSurface
import com.desloth.app.ui.theme.DeSlothTheme
import com.desloth.app.ui.theme.PrimarySky
import com.desloth.app.ui.theme.TextMuted
import com.desloth.app.ui.theme.TextPrimary
import com.desloth.app.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory((application as DeSlothApp).appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeSlothTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    DeSlothDashboardScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions(this)
        viewModel.refreshLedger()
    }
}

@Composable
fun DeSlothDashboardScreen(viewModel: MainViewModel) {
    val ledgerState by viewModel.ledgerState.collectAsState()
    val permissionsState by viewModel.permissionsState.collectAsState()
    val deliverables by viewModel.deliverables.collectAsState()
    val targetApps by viewModel.targetApps.collectAsState()
    val facts by viewModel.facts.collectAsState()
    val bypassState by viewModel.bypassState.collectAsState()
    val usageMap by viewModel.usageMap.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = PrimarySky,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DeSloth",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Text(
                        text = "OFFLINE SYSTEMS ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimarySky,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Stimulation Ledger & Kinetic Friction Interceptor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 1. Permission Status Banner
            item {
                PermissionStatusBanner(
                    permissionsState = permissionsState
                )
            }

            // 2. Large Stimulation Ledger Header
            item {
                LedgerHeaderCard(
                    state = ledgerState,
                    bypassState = bypassState,
                    onRefresh = { viewModel.refreshLedger() }
                )
            }

            // 3. Daily Deliverables & Micro-Tasks Checklist
            item {
                DeliverablesSection(
                    deliverables = deliverables,
                    onToggleComplete = { id, completed ->
                        viewModel.toggleDeliverable(id, completed)
                    },
                    onDelete = { id ->
                        viewModel.deleteDeliverable(id)
                    },
                    onAddDeliverable = { title, units, isMicro ->
                        viewModel.addDeliverable(title, units, isMicro)
                    }
                )
            }

            // 4. Blacklisted Target Apps Picker & Tier Configuration
            item {
                TargetAppsSection(
                    targetApps = targetApps,
                    usageMap = usageMap,
                    onAddTargetApp = { pkg, name, tier ->
                        viewModel.addTargetApp(pkg, name, tier)
                    },
                    onUpdateTier = { pkg, newTier ->
                        viewModel.updateTargetAppTier(pkg, newTier)
                    },
                    onDeleteTargetApp = { pkg ->
                        viewModel.deleteTargetApp(pkg)
                    }
                )
            }

            // 5. Fact Vault Cognitive Anchors
            item {
                FactVaultSection(
                    facts = facts,
                    onAddFact = { statement, goal, deadline, hours ->
                        viewModel.addFact(statement, goal, deadline, hours)
                    },
                    onDeleteFact = { id ->
                        viewModel.deleteFact(id)
                    }
                )
            }

            // 6. Test Suite / Sample Data Harness
            item {
                SeedDataCard(
                    onSeedData = { viewModel.seedSampleData() },
                    onResetTodayCompleted = { viewModel.resetTodayCompletion() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
