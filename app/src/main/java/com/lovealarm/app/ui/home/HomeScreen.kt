package com.lovealarm.app.ui.home

// OWNED BY: home/tiles subagent. Foundation stub so the project compiles.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The board: partner's name in the header, preset tiles then custom tiles, "Add tile" bar at the bottom.
 * Tap a tile = send an alert to the partner.
 */
@Composable
fun HomeScreen(uid: String, pairId: String, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Home (not built yet)")
    }
}
