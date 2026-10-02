package com.lovealarm.app.ui.onboarding

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovealarm.app.data.PAIR_CODE_LENGTH
import com.lovealarm.app.data.UserProfile
import com.lovealarm.app.ui.theme.Blush
import com.lovealarm.app.ui.theme.InkSoft
import com.lovealarm.app.ui.theme.Lavender
import com.lovealarm.app.ui.theme.Mint
import com.lovealarm.app.ui.theme.Peach

/**
 * First-open flow: name -> pairing (create / join with code) -> notification permission.
 * When the user document gets a name and a pairId, RootViewModel switches to Home automatically.
 * [profile] is the current user document (null before it exists); use it to resume a half-finished onboarding.
 * The pairId is only written by the Done button on the last step (see [OnboardingViewModel]).
 */
@Composable
fun OnboardingScreen(uid: String, profile: UserProfile?) {
    val viewModel: OnboardingViewModel = viewModel { OnboardingViewModel(uid, profile) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Love Alarm",
            style = MaterialTheme.typography.titleMedium,
            color = InkSoft,
        )
        when (state.step) {
            OnboardingStep.NAME -> NameStep(state, viewModel)
            OnboardingStep.PAIR -> PairStep(state, viewModel)
            OnboardingStep.NOTIFICATIONS -> NotificationsStep(state, viewModel)
        }
    }
}

// ---------------------------------------------------------------------------- step 1

@Composable
private fun NameStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    StepTitle("What's your name?")
    Text("This is what your person will see on your alerts.", style = MaterialTheme.typography.bodyLarge)
    PastelCard(Blush) {
        TextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Your name") },
            singleLine = true,
            enabled = !state.busy,
            supportingText = { Text("${state.name.length} / $MAX_NAME_LENGTH") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.saveName() }),
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(state.error)
        BusyOr(state.busy) {
            Button(
                onClick = viewModel::saveName,
                enabled = state.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continue") }
        }
    }
}

// ---------------------------------------------------------------------------- step 2

@Composable
private fun PairStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    StepTitle("Pair with your person")
    when (state.pairMode) {
        PairMode.CHECKING -> {
            Text("Checking for an existing pair…", style = MaterialTheme.typography.bodyLarge)
            BusyOr(busy = true) {}
        }

        PairMode.CHOOSE -> {
            Text(
                "One of you creates a pair and shares the code. The other enters it.",
                style = MaterialTheme.typography.bodyLarge,
            )
            PastelCard(Lavender) {
                Text("Create a pair", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("You'll get a 6-character code to send.", style = MaterialTheme.typography.bodyMedium)
                BusyOr(state.busy) {
                    Button(onClick = viewModel::createPair, modifier = Modifier.fillMaxWidth()) {
                        Text("Create a pair")
                    }
                }
            }
            PastelCard(Mint) {
                Text("I have a code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Your person already created a pair.", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = viewModel::showJoin,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Enter code") }
            }
            ErrorText(state.error)
        }

        PairMode.WAITING -> WaitingForPartner(state, viewModel)

        PairMode.JOIN -> {
            PastelCard(Mint) {
                Text("Enter the code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextField(
                    value = state.joinCode,
                    onValueChange = viewModel::onJoinCodeChange,
                    label = { Text("Code") },
                    singleLine = true,
                    enabled = !state.busy,
                    isError = state.error != null,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.joinPair() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                ErrorText(state.error)
                BusyOr(state.busy) {
                    Button(
                        onClick = viewModel::joinPair,
                        enabled = state.joinCode.length == PAIR_CODE_LENGTH,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Join") }
                }
            }
            TextButton(onClick = viewModel::backToChoice, enabled = !state.busy) { Text("Back") }
        }
    }
}

@Composable
private fun WaitingForPartner(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    PastelCard(Lavender) {
        Text("Your code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = state.createdCode,
            style = MaterialTheme.typography.displayMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                clipboard.setText(AnnotatedString(state.createdCode))
                copied = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (copied) "Copied" else "Copy") }
        Text(
            "Send this code to your person. Once they enter it, you're paired.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.size(12.dp))
        Text("Waiting for your person to join…", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
    }
    ErrorText(state.error)
    OutlinedButton(onClick = viewModel::continueWithoutPartner, modifier = Modifier.fillMaxWidth()) {
        Text("Continue now — they can join later")
    }
    TextButton(onClick = viewModel::showJoin) { Text("I have a code instead") }
}

// ---------------------------------------------------------------------------- step 3

@Composable
private fun NotificationsStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(areNotificationsOn(context)) }
    var batteryAllowed by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var askedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedOnce = true
        notificationsOn = areNotificationsOn(context)
    }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        notificationsOn = areNotificationsOn(context)
        batteryAllowed = isIgnoringBatteryOptimizations(context)
    }

    StepTitle("Allow notifications")
    Text(
        "You're paired. Love Alarm needs to show notifications so alerts from your person reach you.",
        style = MaterialTheme.typography.bodyLarge,
    )

    PastelCard(Peach) {
        Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (notificationsOn) {
            Text("Allowed", style = MaterialTheme.typography.bodyLarge, color = InkSoft)
        } else {
            if (askedOnce) {
                Text(
                    "Notifications are still off. Turn them on in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && !askedOnce) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        launchSafely { settingsLauncher.launch(intent) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (askedOnce || Build.VERSION.SDK_INT < 33) "Open notification settings" else "Allow notifications") }
        }
    }

    PastelCard(Mint) {
        Text("Background", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Some phones hold back notifications from apps to save battery. This lets alerts arrive straight away.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (batteryAllowed) {
            Text("Already allowed", style = MaterialTheme.typography.bodyLarge, color = InkSoft)
        } else {
            OutlinedButton(
                onClick = {
                    val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:" + context.packageName))
                    val ok = launchSafely { settingsLauncher.launch(request) }
                    if (!ok) {
                        launchSafely { settingsLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Allow running in the background") }
        }
    }

    ErrorText(state.error)
    BusyOr(state.busy) {
        Button(onClick = viewModel::finish, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

private fun areNotificationsOn(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

/** Runs [block]; returns false if no app on the phone can handle the intent. */
private inline fun launchSafely(block: () -> Unit): Boolean =
    try {
        block()
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

// ---------------------------------------------------------------------------- shared bits

@Composable
private fun StepTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PastelCard(color: Color, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

/** Shows a spinner in place of [content] while [busy]. */
@Composable
private fun BusyOr(busy: Boolean, content: @Composable () -> Unit) {
    if (busy) {
        Column(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
    } else {
        content()
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}
