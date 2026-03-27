package com.angail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.angail.ui.theme.AngailTheme

class MainActivity : ComponentActivity() {
    private var permissionVersion by mutableStateOf(0)
    private lateinit var permissionHandler: PermissionHandler

    private val usageStatsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionHandler.checkUsageStatsPermission()
    }

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionHandler.checkOverlayPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHandler = PermissionHandler(
            context = this,
            usageStatsLauncher = {
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }.let(usageStatsLauncher::launch)
            },
            overlayLauncher = {
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }.let(overlayLauncher::launch)
            },
            onPermissionChanged = { permissionVersion++ }
        )

        setContent {
            AngailTheme {
                val usageStatsGranted = permissionVersion.let { permissionHandler.hasUsageStatsPermission() }
                val overlayGranted = permissionVersion.let { permissionHandler.hasOverlayPermission() }
                MainScreen(
                    permissionHandler = permissionHandler,
                    agentController = remember { AgentController(this@MainActivity) },
                    usageStatsGranted = usageStatsGranted,
                    overlayGranted = overlayGranted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    permissionHandler: PermissionHandler,
    agentController: AgentController,
    usageStatsGranted: Boolean,
    overlayGranted: Boolean
) {
    var isAgentActive by remember { mutableStateOf(false) }
    var socialMediaThreshold by remember { mutableStateOf(30) }
    var screenTimeThreshold by remember { mutableStateOf(120) }

    val allPermissionsGranted = usageStatsGranted && overlayGranted

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Angail - Usage Guardian") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PermissionSection(
                usageStatsGranted = usageStatsGranted,
                overlayGranted = overlayGranted,
                onRequestUsageStats = { permissionHandler.requestUsageStatsPermission() },
                onRequestOverlay = { permissionHandler.requestOverlayPermission() }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (allPermissionsGranted) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (allPermissionsGranted) "Ready to Start" else "Setup Required",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Switch(
                        checked = isAgentActive,
                        onCheckedChange = { 
                            if (allPermissionsGranted) {
                                isAgentActive = it
                                if (it) {
                                    agentController.startAgent(
                                        socialMediaThreshold = socialMediaThreshold,
                                        screenTimeThreshold = screenTimeThreshold
                                    )
                                } else {
                                    agentController.stopAgent()
                                }
                            }
                        },
                        enabled = allPermissionsGranted,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    
                    Text(
                        text = if (isAgentActive) "Agent Active" else "Agent Inactive",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            ThresholdSection(
                socialMediaThreshold = socialMediaThreshold,
                onSocialMediaThresholdChange = { socialMediaThreshold = it },
                screenTimeThreshold = screenTimeThreshold,
                onScreenTimeThresholdChange = { screenTimeThreshold = it },
                enabled = allPermissionsGranted && !isAgentActive
            )
        }
    }
}

@Composable
fun PermissionSection(
    usageStatsGranted: Boolean,
    overlayGranted: Boolean,
    onRequestUsageStats: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium
            )
            
            PermissionItem(
                title = "Usage Stats",
                description = "Monitor app usage",
                granted = usageStatsGranted,
                onRequest = onRequestUsageStats
            )
            
            PermissionItem(
                title = "Overlay",
                description = "Show notifications",
                granted = overlayGranted,
                onRequest = onRequestOverlay
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Button(
            onClick = onRequest,
            enabled = !granted,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
        ) {
            Text(if (granted) "Granted" else "Request")
        }
    }
}

@Composable
fun ThresholdSection(
    socialMediaThreshold: Int,
    onSocialMediaThresholdChange: (Int) -> Unit,
    screenTimeThreshold: Int,
    onScreenTimeThresholdChange: (Int) -> Unit,
    enabled: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Trigger Thresholds",
                style = MaterialTheme.typography.titleMedium
            )
            
            ThresholdSlider(
                label = "Social Media (minutes)",
                value = socialMediaThreshold,
                onValueChange = onSocialMediaThresholdChange,
                range = 5..60,
                enabled = enabled
            )
            
            ThresholdSlider(
                label = "Screen Time (minutes)",
                value = screenTimeThreshold,
                onValueChange = onScreenTimeThresholdChange,
                range = 30..240,
                enabled = enabled
            )
        }
    }
}

@Composable
fun ThresholdSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    enabled: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$value min",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled,
            steps = range.last - range.first
        )
    }
}
