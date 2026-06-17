package com.rpetitto.tvcalendar

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rpetitto.tvcalendar.auth.DeviceCodeResponse
import com.rpetitto.tvcalendar.auth.DeviceFlowAuth
import com.rpetitto.tvcalendar.auth.TokenStore
import com.rpetitto.tvcalendar.data.CalendarRepository
import com.rpetitto.tvcalendar.ui.screens.CalendarUiState
import com.rpetitto.tvcalendar.worker.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** App-level state holder: drives auth flow and feeds the calendar UI. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    /** High-level navigation state for the app. */
    sealed interface AuthState {
        data object NeedsAuth : AuthState
        data class Pairing(val deviceCode: DeviceCodeResponse) : AuthState
        data object Authenticated : AuthState
        data class Error(val message: String) : AuthState
    }

    private val repository = CalendarRepository.create(app)
    private val tokenStore = TokenStore(app)
    private val deviceFlow = DeviceFlowAuth()

    private val _authState = MutableStateFlow<AuthState>(AuthState.NeedsAuth)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _calendarState = MutableStateFlow(CalendarUiState())
    val calendarState: StateFlow<CalendarUiState> = _calendarState.asStateFlow()

    private val today = MutableStateFlow(LocalDate.now())
    private var eventsJob: Job? = null
    private var lastPausedAt: Long = 0L

    init {
        if (tokenStore.hasTokens()) {
            onAuthenticated()
        } else {
            startPairing()
        }
        observeMidnight()
    }

    /** Begins the OAuth Device Flow: request a code, then poll for the token. */
    fun startPairing() {
        val clientId = BuildConfig.GOOGLE_CLIENT_ID
        val clientSecret = BuildConfig.GOOGLE_CLIENT_SECRET
        if (clientId.isBlank()) {
            _authState.value = AuthState.Error("Missing OAuth client id. Check local.properties.")
            return
        }

        viewModelScope.launch {
            val code = try {
                deviceFlow.requestDeviceCode(clientId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request device code", e)
                _authState.value = AuthState.Error("Could not start pairing.")
                return@launch
            }
            _authState.value = AuthState.Pairing(code)

            deviceFlow.pollForToken(clientId, clientSecret, code.deviceCode, code.interval)
                .collect { result ->
                    when (result) {
                        is DeviceFlowAuth.TokenResult.Pending -> Unit
                        is DeviceFlowAuth.TokenResult.Success -> {
                            tokenStore.saveTokens(
                                accessToken = result.accessToken,
                                refreshToken = result.refreshToken,
                                expiresAt = result.expiresAt,
                            )
                            onAuthenticated()
                        }
                        is DeviceFlowAuth.TokenResult.Error -> {
                            _authState.value = AuthState.Error(result.reason)
                        }
                    }
                }
        }
    }

    private fun onAuthenticated() {
        _authState.value = AuthState.Authenticated
        SyncScheduler.schedulePeriodicSync(getApplication())
        triggerSync()
        observeEvents()
    }

    private fun triggerSync() {
        viewModelScope.launch {
            when (val result = repository.syncEvents()) {
                is CalendarRepository.SyncResult.NeedsAuth -> {
                    tokenStore.clearTokens()
                    startPairing()
                }
                is CalendarRepository.SyncResult.Success ->
                    _calendarState.update { it.copy(isLoading = false) }
                else -> {
                    Log.w(TAG, "Sync failed: $result")
                    _calendarState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeEvents() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            today.flatMapLatest { day ->
                val weekStart = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                combine(
                    repository.observeEventsForDay(day),
                    repository.observeEventsForWeek(weekStart),
                ) { dayEvents, weekEvents ->
                    Triple(day, dayEvents, weekEvents) to weekStart
                }
            }.collect { (triple, weekStart) ->
                val (day, dayEvents, weekEvents) = triple
                _calendarState.update {
                    it.copy(
                        today = day,
                        weekStart = weekStart,
                        dayEvents = dayEvents,
                        weekEvents = weekEvents,
                    )
                }
            }
        }
    }

    private fun observeMidnight() {
        viewModelScope.launch {
            repository.midnightTicks().collect {
                today.value = LocalDate.now()
            }
        }
    }

    /** Called from the activity's per-minute tick; cheaply guards day rollover. */
    fun onMinuteTick() {
        val current = LocalDate.now()
        if (current != today.value) today.value = current
    }

    fun onPaused() {
        lastPausedAt = System.currentTimeMillis()
    }

    /** On resume after a long background gap, refresh before showing stale data. */
    fun onResumed() {
        if (_authState.value !is AuthState.Authenticated) return
        val gap = System.currentTimeMillis() - lastPausedAt
        if (lastPausedAt != 0L && gap > STALE_THRESHOLD_MS) {
            triggerSync()
            SyncScheduler.syncNow(getApplication())
        }
        onMinuteTick()
    }

    fun retry() {
        _authState.value = AuthState.NeedsAuth
        startPairing()
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val STALE_THRESHOLD_MS = 30 * 60 * 1000L
    }
}
