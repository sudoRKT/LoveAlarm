package com.lovealarm.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovealarm.app.AppGraph
import com.lovealarm.app.data.UserProfile
import com.lovealarm.app.push.TokenRegistrar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** What the app should show at the top level. Decided from sign-in state and the user document. */
sealed interface RootState {
    data object Loading : RootState
    data class Error(val message: String) : RootState

    /** Signed in but not yet paired (or no name yet). [profile] may be null on the very first open. */
    data class Onboarding(val uid: String, val profile: UserProfile?) : RootState

    /** Signed in and paired. */
    data class Ready(val uid: String, val profile: UserProfile) : RootState
}

class RootViewModel : ViewModel() {
    private val _state = MutableStateFlow<RootState>(RootState.Loading)
    val state: StateFlow<RootState> = _state

    init {
        start()
    }

    fun retry() {
        _state.value = RootState.Loading
        start()
    }

    private fun start() {
        viewModelScope.launch {
            val uid = try {
                AppGraph.auth.ensureSignedIn()
            } catch (e: Exception) {
                _state.value = RootState.Error(e.message ?: "Couldn't sign in.")
                return@launch
            }
            launch { TokenRegistrar.refresh(uid) }
            AppGraph.users.observeUser(uid)
                .catch { e -> _state.value = RootState.Error(e.message ?: "Couldn't load your profile.") }
                .collect { profile ->
                    _state.value = if (profile != null && profile.isPaired && profile.name.isNotBlank()) {
                        RootState.Ready(uid, profile)
                    } else {
                        RootState.Onboarding(uid, profile)
                    }
                }
        }
    }
}
