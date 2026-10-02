package com.lovealarm.app.ui.home

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovealarm.app.AppGraph
import com.lovealarm.app.data.LoveAlarmException
import com.lovealarm.app.data.Presets
import com.lovealarm.app.data.Tile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class HomeUiState(
    /** Partner's display name. "" while unknown or when there is no partner yet. */
    val partnerName: String = "",
    /** Partner's uid, or null while the pair is not full (or not loaded yet). */
    val partnerUid: String? = null,
    /** Presets first, then this user's custom tiles. */
    val tiles: List<Tile> = Presets.tiles,
    /** True until the first pair snapshot and the first tiles snapshot have both arrived (or failed). */
    val isLoading: Boolean = true,
    /** One-shot snackbar text. Cleared with [HomeViewModel.consumeMessage]. */
    val message: String? = null,
) {
    val hasPartner: Boolean get() = partnerUid != null
}

private data class PartnerInfo(val uid: String?, val name: String)

class HomeViewModel(
    private val uid: String,
    private val pairId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** tileId -> elapsedRealtime of the last accepted send. Touched on the main thread only. */
    private val lastSentAt = mutableMapOf<String, Long>()

    private var partnerLoaded = false
    private var tilesLoaded = false

    init {
        observePartner()
        observeTiles()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observePartner() {
        if (pairId.isBlank()) {
            partnerLoaded = true
            refreshLoading()
            return
        }
        AppGraph.pairs.observePair(pairId)
            .map { pair -> pair?.partnerOf(uid) }
            .distinctUntilChanged()
            .flatMapLatest { partnerUid ->
                if (partnerUid == null) {
                    flowOf(PartnerInfo(uid = null, name = ""))
                } else {
                    AppGraph.users.observeUser(partnerUid)
                        .map { profile -> PartnerInfo(uid = partnerUid, name = profile?.name.orEmpty()) }
                        .catch {
                            // Keep the partner uid so sending still works even if their profile can't be read.
                            emit(PartnerInfo(uid = partnerUid, name = ""))
                            showMessage("Couldn't load your person's name.")
                        }
                }
            }
            .onEach { info ->
                partnerLoaded = true
                _state.update { it.copy(partnerUid = info.uid, partnerName = info.name) }
                refreshLoading()
            }
            .catch { e ->
                partnerLoaded = true
                refreshLoading()
                showMessage((e as? LoveAlarmException)?.message ?: "Couldn't load your pair.")
            }
            .launchIn(viewModelScope)
    }

    private fun observeTiles() {
        if (pairId.isBlank()) {
            tilesLoaded = true
            refreshLoading()
            return
        }
        AppGraph.tiles.observeCustomTiles(pairId, uid)
            .onEach { custom ->
                tilesLoaded = true
                _state.update { it.copy(tiles = Presets.tiles + custom) }
                refreshLoading()
            }
            .catch { e ->
                tilesLoaded = true
                refreshLoading()
                showMessage((e as? LoveAlarmException)?.message ?: "Couldn't load your tiles.")
            }
            .launchIn(viewModelScope)
    }

    private fun refreshLoading() {
        val loading = !(partnerLoaded && tilesLoaded)
        _state.update { it.copy(isLoading = loading) }
    }

    private fun showMessage(text: String) {
        _state.update { it.copy(message = text) }
    }

    /**
     * Sends [tile] to the partner. Returns true if the tap was accepted (use it to fire the haptic tick),
     * false if it was ignored as a repeat inside the 1.5 s window or there is no partner.
     */
    fun sendTile(tile: Tile): Boolean {
        val now = SystemClock.elapsedRealtime()
        val last = lastSentAt[tile.id]
        if (last != null && now - last < SEND_COOLDOWN_MS) return false

        val partnerUid = _state.value.partnerUid
        if (partnerUid == null) {
            showMessage("You're not paired with anyone yet.")
            return false
        }

        lastSentAt[tile.id] = now
        viewModelScope.launch {
            try {
                AppGraph.alerts.sendAlert(
                    pairId = pairId,
                    fromUid = uid,
                    toUid = partnerUid,
                    text = tile.text,
                )
                showMessage("Sent: ${tile.text}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: LoveAlarmException) {
                showMessage(e.message ?: "Couldn't send.")
            } catch (e: Exception) {
                showMessage("Couldn't send. Check your connection and try again.")
            }
        }
        return true
    }

    fun addTile(text: String) {
        viewModelScope.launch {
            try {
                AppGraph.tiles.addTile(pairId, uid, text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: LoveAlarmException) {
                showMessage(e.message ?: "Couldn't add the tile.")
            } catch (e: Exception) {
                showMessage("Couldn't add the tile.")
            }
        }
    }

    fun deleteTile(tile: Tile) {
        if (tile.isPreset) return
        viewModelScope.launch {
            try {
                AppGraph.tiles.deleteTile(pairId, tile.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: LoveAlarmException) {
                showMessage(e.message ?: "Couldn't delete the tile.")
            } catch (e: Exception) {
                showMessage("Couldn't delete the tile.")
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private companion object {
        const val SEND_COOLDOWN_MS = 1_500L
    }
}
