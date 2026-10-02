package com.lovealarm.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovealarm.app.data.Tile
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The board: partner's name in the header, preset tiles then custom tiles, "Add tile" bar at the bottom.
 * Tap a tile = send an alert to the partner. Long-press a custom tile = delete it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(uid: String, pairId: String, onOpenSettings: () -> Unit) {
    val viewModel: HomeViewModel = viewModel { HomeViewModel(uid, pairId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbarJob = remember { mutableStateOf<Job?>(null) }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var tilePendingDelete by remember { mutableStateOf<Tile?>(null) }

    // Show each message once. The newest message replaces whatever snackbar is on screen.
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarJob.value?.cancel()
        snackbarJob.value = scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
        viewModel.consumeMessage()
    }

    val subtitle = when {
        state.isLoading && !state.hasPartner -> ""
        !state.hasPartner -> "Waiting for your person…"
        state.partnerName.isNotBlank() -> state.partnerName
        else -> "Your person"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Love Alarm")
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add tile")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items = state.tiles, key = { _, tile -> tile.id }) { index, tile ->
                TileRow(
                    tile = tile,
                    index = index,
                    onClick = {
                        if (viewModel.sendTile(tile)) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onLongClick = if (tile.isPreset) null else ({ tilePendingDelete = tile }),
                )
            }
        }
    }

    if (showAddDialog) {
        AddTileDialog(
            onAdd = { text ->
                viewModel.addTile(text)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    tilePendingDelete?.let { tile ->
        AlertDialog(
            onDismissRequest = { tilePendingDelete = null },
            title = { Text("Delete this tile?") },
            text = { Text(tile.text) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTile(tile)
                        tilePendingDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { tilePendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
