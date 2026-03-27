package com.angail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.angail.ui.theme.AngailTheme
import kotlinx.coroutines.launch

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
                val githubAuth = remember { GitHubAuth(this@MainActivity) }
                MainScreen(
                    permissionHandler = permissionHandler,
                    agentController = remember { AgentController(this@MainActivity, githubAuth) },
                    usageStatsGranted = usageStatsGranted,
                    overlayGranted = overlayGranted,
                    githubAuth = githubAuth
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
    overlayGranted: Boolean,
    githubAuth: GitHubAuth
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
            GitHubSection(githubAuth = githubAuth)

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

private enum class GitHubAuthState {
    LoggedOut, RequestingCode, AwaitingUser, LoggedIn, Error
}

@Composable
fun GitHubSection(githubAuth: GitHubAuth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var authState by remember {
        mutableStateOf(
            if (githubAuth.isAuthenticated()) GitHubAuthState.LoggedIn
            else GitHubAuthState.LoggedOut
        )
    }
    var userCode by remember { mutableStateOf("") }
    var verificationUri by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var login by remember { mutableStateOf(githubAuth.getLogin() ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("GitHub / Copilot", style = MaterialTheme.typography.titleMedium)

            when (authState) {
                GitHubAuthState.LoggedOut -> {
                    Text(
                        "Sign in to enable AI-generated nudges via GitHub Copilot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            authState = GitHubAuthState.RequestingCode
                            scope.launch {
                                val code = githubAuth.requestDeviceCode()
                                if (code == null) {
                                    errorMessage = "Failed to contact GitHub. Check your connection."
                                    authState = GitHubAuthState.Error
                                    return@launch
                                }
                                userCode = code.userCode
                                verificationUri = code.verificationUri
                                authState = GitHubAuthState.AwaitingUser

                                val token = githubAuth.pollForToken(code)
                                if (token != null) {
                                    login = githubAuth.getLogin() ?: ""
                                    authState = GitHubAuthState.LoggedIn
                                } else {
                                    errorMessage = "Login timed out. Please try again."
                                    authState = GitHubAuthState.Error
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign in with GitHub")
                    }
                }

                GitHubAuthState.RequestingCode -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text(
                        "Contacting GitHub...",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                GitHubAuthState.AwaitingUser -> {
                    Text(
                        "1. Open $verificationUri in your browser",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "2. Enter this code:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = userCode,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedButton(onClick = {
                            val clip = ClipData.newPlainText("GitHub code", userCode)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                .setPrimaryClip(clip)
                        }) {
                            Text("Copy")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Waiting for authorization…", style = MaterialTheme.typography.bodySmall)
                    }
                }

                GitHubAuthState.LoggedIn -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Signed in", style = MaterialTheme.typography.bodyMedium)
                            if (login.isNotEmpty()) {
                                Text(
                                    "@$login",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(onClick = {
                            githubAuth.logout()
                            login = ""
                            authState = GitHubAuthState.LoggedOut
                        }) {
                            Text("Sign out")
                        }
                    }
                    Text(
                        "AI notifications are enabled via GitHub Models.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                GitHubAuthState.Error -> {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { authState = GitHubAuthState.LoggedOut },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
