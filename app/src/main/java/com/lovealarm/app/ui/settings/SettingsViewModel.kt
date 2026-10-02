package com.lovealarm.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovealarm.app.AppGraph
import com.lovealarm.app.push.TokenRegistrar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    /** Null while loading; empty string if the code couldn't be found. */
    val pairCode: String? = null,
    val isSending: Boolean = false,
    val testStatus: String? = null,
    val message: String? = null,
)

class SettingsViewModel(
    private val uid: String,
    private val pairId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        loadPairCode()
    }

    private fun loadPairCode() {
        viewModelScope.launch {
            val code = try {
                AppGraph.pairs.getPairCode(pairId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Couldn't load the pairing code.") }
                null
            }
            _state.update { it.copy(pairCode = code.orEmpty()) }
        }
    }

    /** Writes an alert addressed to this same phone, so the whole server → FCM → notification chain can be checked with one phone. */
    fun sendTestAlert() {
        if (_state.value.isSending) return
        _state.update { it.copy(isSending = true, testStatus = "Sending…") }
        viewModelScope.launch {
            val status = try {
                AppGraph.alerts.sendAlert(
                    pairId = pairId,
                    fromUid = uid,
                    toUid = uid,
                    text = "Test alert from Love Alarm 💕",
                    waitForServer = true,
                )
                "Test alert written. If the server is running you'll get a notification within a few seconds."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: "Couldn't send the test alert."
            }
            _state.update { it.copy(isSending = false, testStatus = status) }
        }
    }

    fun refreshToken() {
        viewModelScope.launch {
            TokenRegistrar.refresh(uid)
            val message = if (TokenRegistrar.lastToken != null) "Push token refreshed" else "Couldn't get a push token"
            _state.update { it.copy(message = message) }
        }
    }

    fun showMessage(message: String) {
        _state.update { it.copy(message = message) }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }
}
