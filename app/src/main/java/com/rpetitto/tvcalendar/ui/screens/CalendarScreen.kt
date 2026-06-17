package com.rpetitto.tvcalendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.ui.components.AgendaView
import com.rpetitto.tvcalendar.ui.components.ClockWidget
import com.rpetitto.tvcalendar.ui.components.DateHeader
import com.rpetitto.tvcalendar.ui.components.WeekView
import com.rpetitto.tvcalendar.ui.theme.AmbientBackground
import com.rpetitto.tvcalendar.ui.theme.Divider
import com.rpetitto.tvcalendar.ui.theme.MutedGray
import com.rpetitto.tvcalendar.ui.theme.TvType
import java.time.LocalDate

/** Immutable snapshot the calendar UI renders. */
data class CalendarUiState(
    val today: LocalDate = LocalDate.now(),
    val weekStart: LocalDate = LocalDate.now(),
    val dayEvents: List<EventEntity> = emptyList(),
    val weekEvents: Map<LocalDate, List<EventEntity>> = emptyMap(),
    val calendarName: String = "",
    val isLoading: Boolean = true,
)

/**
 * Ambient main display: week strip on the left, today's agenda on the right.
 * Nothing here is focusable — it's a pure, remote-free display.
 */
@Composable
fun CalendarScreen(state: CalendarUiState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    // Scroll the agenda so the next upcoming event sits near the top.
    LaunchedEffect(state.dayEvents) {
        val nowMillis = System.currentTimeMillis()
        val timed = state.dayEvents.filterNot { it.isAllDay }.sortedBy { it.startMillis }
        val allDayCount = state.dayEvents.count { it.isAllDay }
        val upcomingIndex = timed.indexOfFirst { it.endMillis >= nowMillis }
        if (upcomingIndex >= 0) {
            listState.scrollToItem(allDayCount + upcomingIndex)
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(AmbientBackground),
    ) {
        WeekView(
            weekStart = state.weekStart,
            today = state.today,
            eventsByDay = state.weekEvents,
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Divider),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClockWidget()
                if (state.calendarName.isNotEmpty()) {
                    Text(
                        text = state.calendarName,
                        style = TvType.dateLabel,
                        color = MutedGray,
                    )
                }
            }

            DateHeader(
                date = state.today,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            )

            AgendaView(
                events = state.dayEvents,
                listState = listState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
