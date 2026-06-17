package com.rpetitto.tvcalendar

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rpetitto.tvcalendar.auth.DeviceCodeResponse
import com.rpetitto.tvcalendar.auth.DeviceFlowAuth
import com.rpetitto.tvcalendar.auth.TokenStore
import com.rpetitto.tvcalendar.data.CalendarRepository
import com.rpetitto.tvcalendar.ui.CalendarUiState
import com.rpetitto.tvcalendar.ui.CalendarView
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

    /** High-level auth navigation state. */
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

    /** Drives event-observation: emits whenever the focused day changes. */
    private val focusDate = MutableStateFlow(LocalDate.now())

    /** Tracks which view the user came from so Back returns there. */
    private var previousView: CalendarView = CalendarView.Week
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

    // --- Auth -------------------------------------------------------------

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

    // --- Event observation -----------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeEvents() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            focusDate.flatMapLatest { focus ->
                val weekStart = focus.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                combine(
                    repository.observeEventsForDay(focus),
                    repository.observeEventsForWeek(weekStart),
                    repository.observeEventsForMonth(focus),
                ) { dayEvents, weekEvents, monthEvents ->
                    EventSnapshot(focus, dayEvents, weekEvents, monthEvents)
                }
            }.collect { snap ->
                _calendarState.update {
                    it.copy(
                        dayEvents = snap.dayEvents,
                        weekEvents = snap.weekEvents,
                        monthEvents = snap.monthEvents,
                    )
                }
            }
        }
    }

    private data class EventSnapshot(
        val focus: LocalDate,
        val dayEvents: List<com.rpetitto.tvcalendar.data.local.EventEntity>,
        val weekEvents: Map<LocalDate, List<com.rpetitto.tvcalendar.data.local.EventEntity>>,
        val monthEvents: Map<LocalDate, List<com.rpetitto.tvcalendar.data.local.EventEntity>>,
    )

    private fun observeMidnight() {
        viewModelScope.launch {
            repository.midnightTicks().collect {
                rollToToday()
            }
        }
    }

    private fun rollToToday() {
        val now = LocalDate.now()
        _calendarState.update { it.copy(today = now) }
        // If the user is still on today's agenda, advance that too.
        if (_calendarState.value.agendaDate.isBefore(now)) {
            _calendarState.update { it.copy(agendaDate = now) }
        }
    }

    // --- Navigation -------------------------------------------------------

    fun setView(view: CalendarView) {
        val current = _calendarState.value.view
        if (current == view) return
        previousView = current
        _calendarState.update { it.copy(view = view) }
    }

    /** D-pad-driven day focus in Week/Month. Cheap; also drives event window. */
    fun setFocusedDate(date: LocalDate) {
        _calendarState.update { it.copy(focusedDate = date) }
        focusDate.value = date
    }

    /** Open Agenda for a specific day (from Week/Month "select"). */
    fun openAgenda(date: LocalDate) {
        previousView = _calendarState.value.view
        _calendarState.update {
            it.copy(view = CalendarView.Agenda, agendaDate = date, focusedDate = date)
        }
        focusDate.value = date
    }

    /**
     * Handle a Back press. Returns true if the press was consumed (caller stays
     * in-app), false if the activity should be allowed to finish.
     */
    fun onBackPressed(): Boolean {
        val state = _calendarState.value
        return when (state.view) {
            CalendarView.Agenda -> {
                _calendarState.update { it.copy(view = previousView) }
                true
            }
            CalendarView.Month -> {
                _calendarState.update { it.copy(view = CalendarView.Week) }
                true
            }
            CalendarView.Week -> false
        }
    }

    // --- Lifecycle hooks --------------------------------------------------

    fun onMinuteTick() {
        val current = LocalDate.now()
        if (current != _calendarState.value.today) rollToToday()
    }

    fun onPaused() {
        lastPausedAt = System.currentTimeMillis()
    }

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
