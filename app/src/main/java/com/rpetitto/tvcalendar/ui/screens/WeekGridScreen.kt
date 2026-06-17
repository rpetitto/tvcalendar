package com.rpetitto.tvcalendar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.ui.components.DayCell
import com.rpetitto.tvcalendar.ui.components.isSelectKeyDown
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Week view: 7 wide focusable day cells. Today auto-focused on first show.
 * Left/Right moves day-by-day across weeks; Up/Down moves week-by-week.
 * Select opens that day in Agenda view.
 */
@Composable
fun WeekGridScreen(
    today: LocalDate,
    focusedDate: LocalDate,
    weekEvents: Map<LocalDate, List<EventEntity>>,
    onFocusDate: (LocalDate) -> Unit,
    onActivateDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekStart = focusedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val days = remember(weekStart) { (0 until 7).map { weekStart.plusDays(it.toLong()) } }
    val focusRequesters = remember { List(7) { FocusRequester() } }

    val focusIndex = days.indexOf(focusedDate).coerceAtLeast(0)
    LaunchedEffect(focusedDate) {
        runCatching { focusRequesters[focusIndex].requestFocus() }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEachIndexed { index, day ->
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged { if (it.isFocused) onFocusDate(day) }
                    .onPreviewKeyEvent { event ->
                        if (event.isSelectKeyDown()) {
                            onActivateDate(day); true
                        } else false
                    }
                    .focusable(interactionSource = interactionSource)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        onActivateDate(day)
                    },
            ) {
                DayCell(
                    day = day,
                    isToday = day == today,
                    isFocused = day == focusedDate,
                    isInCurrentMonth = true,
                    events = weekEvents[day].orEmpty(),
                    showWeekday = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
