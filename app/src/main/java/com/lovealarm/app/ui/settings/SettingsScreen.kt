package com.lovealarm.app.ui.settings

// OWNED BY: FCM subagent. Foundation stub so the project compiles.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Milestone 1 settings: "Send a test alert to myself", show pairing code, notification/battery help.
 * Name, time zones and unpair arrive in milestone 2.
 */
@Composable
fun SettingsScreen(uid: String, pairId: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings (not built yet)")
    }
}
