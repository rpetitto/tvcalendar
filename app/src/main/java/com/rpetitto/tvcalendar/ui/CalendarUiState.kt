package com.rpetitto.tvcalendar.ui

import com.rpetitto.tvcalendar.data.local.EventEntity
import java.time.LocalDate

/** Top-level view the user is currently looking at. */
enum class CalendarView { Week, Month, Agenda }

/** Immutable snapshot the calendar UI renders. */
data class CalendarUiState(
    val view: CalendarView = CalendarView.Week,
    /** Real-world today, updated at midnight. */
    val today: LocalDate = LocalDate.now(),
    /**
     * Day that holds D-pad focus in Week/Month. Drives which week/month is
     * displayed; clicking Select opens [agendaDate].
     */
    val focusedDate: LocalDate = LocalDate.now(),
    /** Day shown in Agenda view. Defaults to today. */
    val agendaDate: LocalDate = LocalDate.now(),
    val dayEvents: List<EventEntity> = emptyList(),
    val weekEvents: Map<LocalDate, List<EventEntity>> = emptyMap(),
    val monthEvents: Map<LocalDate, List<EventEntity>> = emptyMap(),
    val isLoading: Boolean = true,
)
