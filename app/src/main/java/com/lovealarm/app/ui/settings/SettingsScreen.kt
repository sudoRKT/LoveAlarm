package com.lovealarm.app.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovealarm.app.push.Notifications
import com.lovealarm.app.ui.theme.Blush
import com.lovealarm.app.ui.theme.Ink
import com.lovealarm.app.ui.theme.InkSoft
import com.lovealarm.app.ui.theme.Lavender
import com.lovealarm.app.ui.theme.Mint
import com.lovealarm.app.ui.theme.Peach
import com.lovealarm.app.ui.theme.Sky

/**
 * Milestone 1 settings: "Send a test alert to myself", show pairing code, notification/battery help.
 * Name, time zones and unpair arrive in milestone 2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(uid: String, pairId: String, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel { SettingsViewModel(uid, pairId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Bumped on resume and after the permission dialog closes, so the status lines re-read the system state.
    var refreshKey by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refreshKey++
        onPauseOrDispose { }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshKey++
    }

    val notificationsAllowed = remember(refreshKey) {
        Notifications.hasPermission(context) && Notifications.areNotificationsEnabled(context)
    }
    val ignoringBatteryOptimizations = remember(refreshKey) { isIgnoringBatteryOptimizations(context) }
    val isXiaomiFamily = remember {
        val names = listOf(Build.MANUFACTURER.lowercase(), Build.BRAND.lowercase())
        names.any { it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") }
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            vm.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCard(title = "Test notifications", color = Lavender) {
                Text(
                    "Sends an alert to this phone through the server, so you can check the whole chain with one phone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { vm.sendTestAlert() },
                    enabled = !state.isSending,
                ) {
                    Text("Send a test alert to myself")
                }
                state.testStatus?.let { status ->
                    Text(status, style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
            }

            SettingsCard(title = "Notification permission", color = Mint) {
                Text(
                    if (notificationsAllowed) "Notifications are allowed." else "Notifications are off, so alerts won't show.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!notificationsAllowed) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !Notifications.hasPermission(context)
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                openNotificationSettings(context)
                            }
                        },
                    ) {
                        Text("Allow notifications")
                    }
                    TextButton(onClick = { openNotificationSettings(context) }) {
                        Text("Open notification settings")
                    }
                }
            }

            SettingsCard(title = "Background", color = Peach) {
                Text(
                    if (ignoringBatteryOptimizations) {
                        "Love Alarm is allowed to run in the background."
                    } else {
                        "Battery saving may delay alerts while the app is closed."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!ignoringBatteryOptimizations) {
                    Button(onClick = { requestIgnoreBatteryOptimizations(context) }) {
                        Text("Allow running in the background")
                    }
                }
                if (isXiaomiFamily) {
                    Text(
                        "On Xiaomi/Redmi/POCO also turn on Autostart for Love Alarm in Security app → Permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }

            SettingsCard(title = "Pairing code", color = Sky) {
                val code = state.pairCode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (code.isNullOrBlank()) "—" else code,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!code.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(code))
                                vm.showMessage("Code copied")
                            },
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy")
                        }
                    }
                }
            }

            SettingsCard(title = "Push token", color = Blush) {
                Text(
                    "Only needed if alerts stop arriving.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                TextButton(onClick = { vm.refreshToken() }) {
                    Text("Refresh push token")
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color, contentColor = Ink),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return power.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    safeStart(context, intent)
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}"),
    )
    if (!safeStart(context, intent)) {
        safeStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

private fun safeStart(context: Context, intent: Intent): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
