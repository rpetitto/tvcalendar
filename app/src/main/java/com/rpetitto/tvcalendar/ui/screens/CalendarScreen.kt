package com.rpetitto.tvcalendar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rpetitto.tvcalendar.ui.CalendarUiState
import com.rpetitto.tvcalendar.ui.CalendarView
import com.rpetitto.tvcalendar.ui.components.ClockWidget
import com.rpetitto.tvcalendar.ui.components.ViewSwitcher
import com.rpetitto.tvcalendar.ui.theme.AmbientBackground
import java.time.LocalDate

/** Top-level calendar surface: clock + view-switcher tabs + the active view. */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onViewSelected: (CalendarView) -> Unit,
    onFocusDate: (LocalDate) -> Unit,
    onActivateDate: (LocalDate) -> Unit,
    onBack: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.view != CalendarView.Week) {
        onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmbientBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ClockWidget()
            ViewSwitcher(current = state.view, onSelect = onViewSelected)
        }

        when (state.view) {
            CalendarView.Week -> WeekGridScreen(
                today = state.today,
                focusedDate = state.focusedDate,
                weekEvents = state.weekEvents,
                onFocusDate = onFocusDate,
                onActivateDate = onActivateDate,
                modifier = Modifier.fillMaxSize(),
            )
            CalendarView.Month -> MonthGridScreen(
                today = state.today,
                focusedDate = state.focusedDate,
                monthEvents = state.monthEvents,
                onFocusDate = onFocusDate,
                onActivateDate = onActivateDate,
                modifier = Modifier.fillMaxSize(),
            )
            CalendarView.Agenda -> AgendaScreen(
                date = state.agendaDate,
                today = state.today,
                events = state.dayEvents,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
