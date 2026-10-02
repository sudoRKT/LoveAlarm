package com.lovealarm.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovealarm.app.AppGraph
import com.lovealarm.app.data.LoveAlarmException
import com.lovealarm.app.data.PAIR_CODE_LENGTH
import com.lovealarm.app.data.PairInfo
import com.lovealarm.app.data.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone

const val MAX_NAME_LENGTH = 30

enum class OnboardingStep { NAME, PAIR, NOTIFICATIONS }

/** Sub-screens of the pair step. */
enum class PairMode {
    /** Looking for a pair this user already belongs to (app closed mid-onboarding). */
    CHECKING,

    /** "Create a pair" or "I have a code". */
    CHOOSE,

    /** A pair was created; showing its code and waiting for the other person. */
    WAITING,

    /** Typing a code to join. */
    JOIN,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.NAME,
    val name: String = "",
    val pairMode: PairMode = PairMode.CHECKING,
    /** The code of the pair this user created, shown while waiting. */
    val createdCode: String = "",
    /** What the user has typed into the join field. */
    val joinCode: String = "",
    /** The pair this user is in. Only written to users/{uid} when they press Done. */
    val pairId: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

/**
 * Drives the three onboarding steps. users/{uid}.pairId is written only by [finish], because
 * RootViewModel leaves onboarding as soon as that field appears and step 3 would never show.
 */
class OnboardingViewModel(
    private val uid: String,
    profile: UserProfile?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingUiState(
            step = if (profile?.name.isNullOrBlank()) OnboardingStep.NAME else OnboardingStep.PAIR,
            name = profile?.name.orEmpty(),
            pairId = profile?.pairId.orEmpty(),
        ),
    )
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private var waitJob: Job? = null

    init {
        if (_state.value.step == OnboardingStep.PAIR) enterPairStep()
    }

    // ------------------------------------------------------------------ step 1: name

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value.take(MAX_NAME_LENGTH), error = null) }
    }

    fun saveName() {
        val name = _state.value.name.trim()
        if (name.isEmpty() || _state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                AppGraph.users.upsertUser(uid, name = name, timeZone = TimeZone.getDefault().id)
                _state.update { it.copy(busy = false, step = OnboardingStep.PAIR) }
                enterPairStep()
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = messageOf(e)) }
            }
        }
    }

    // ------------------------------------------------------------------ step 2: pair

    /** Resumes from what is already known: a pairId on the profile, or a pair found by membership. */
    private fun enterPairStep() {
        if (_state.value.pairId.isNotBlank()) {
            _state.update { it.copy(step = OnboardingStep.NOTIFICATIONS) }
            return
        }
        _state.update { it.copy(pairMode = PairMode.CHECKING, busy = true, error = null) }
        viewModelScope.launch {
            val found: List<PairInfo> = try {
                AppGraph.pairs.findPairsForUser(uid)
            } catch (e: Exception) {
                // Not fatal: the user can still create or join.
                emptyList()
            }
            val full = found.firstOrNull { it.isFull }
            val waiting = found.firstOrNull { !it.isFull }
            when {
                full != null -> goToNotifications(full.id)
                waiting != null -> {
                    val code = try {
                        AppGraph.pairs.getPairCode(waiting.id)
                    } catch (e: Exception) {
                        null
                    }
                    if (code.isNullOrBlank()) {
                        _state.update { it.copy(pairMode = PairMode.CHOOSE, busy = false) }
                    } else {
                        startWaiting(waiting.id, code)
                    }
                }
                else -> _state.update { it.copy(pairMode = PairMode.CHOOSE, busy = false) }
            }
        }
    }

    fun createPair() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val created = AppGraph.pairs.createPairWithId(uid)
                startWaiting(created.pairId, created.code)
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = messageOf(e)) }
            }
        }
    }

    private fun startWaiting(pairId: String, code: String) {
        _state.update {
            it.copy(pairMode = PairMode.WAITING, createdCode = code, pairId = pairId, busy = false, error = null)
        }
        waitJob?.cancel()
        waitJob = viewModelScope.launch {
            AppGraph.pairs.observePair(pairId)
                .catch { e -> _state.update { it.copy(error = messageOf(e)) } }
                .collect { pair ->
                    if (pair != null && pair.isFull && _state.value.step == OnboardingStep.PAIR) {
                        goToNotifications(pairId)
                    }
                }
        }
    }

    fun showJoin() {
        waitJob?.cancel()
        _state.update { it.copy(pairMode = PairMode.JOIN, error = null, busy = false, pairId = "", createdCode = "") }
    }

    fun backToChoice() {
        waitJob?.cancel()
        _state.update { it.copy(pairMode = PairMode.CHOOSE, error = null, busy = false, pairId = "", createdCode = "") }
    }

    fun onJoinCodeChange(value: String) {
        val cleaned = value.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(PAIR_CODE_LENGTH)
        _state.update { it.copy(joinCode = cleaned, error = null) }
    }

    fun joinPair() {
        val code = _state.value.joinCode
        if (code.length != PAIR_CODE_LENGTH || _state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val pairId = AppGraph.pairs.joinPair(uid, code)
                goToNotifications(pairId)
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = messageOf(e)) }
            }
        }
    }

    private fun goToNotifications(pairId: String) {
        waitJob?.cancel()
        _state.update {
            it.copy(step = OnboardingStep.NOTIFICATIONS, pairId = pairId, busy = false, error = null)
        }
    }

    // ------------------------------------------------------------------ step 3: notifications

    /** Writes the pairId to users/{uid}. RootViewModel then switches to Home on its own. */
    fun finish() {
        val pairId = _state.value.pairId
        if (pairId.isBlank() || _state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                AppGraph.users.upsertUser(uid, timeZone = TimeZone.getDefault().id, pairId = pairId)
                // Stay busy: this screen is about to be replaced by Home.
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = messageOf(e)) }
            }
        }
    }

    private fun messageOf(e: Throwable): String =
        if (e is LoveAlarmException) {
            e.message ?: "Something went wrong. Please try again."
        } else {
            "Something went wrong. Please try again."
        }
}
