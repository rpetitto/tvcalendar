package com.rpetitto.tvcalendar.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.ui.components.AgendaView
import com.rpetitto.tvcalendar.ui.components.DateHeader
import java.time.LocalDate

/** Single-day agenda; auto-scrolls to the next upcoming event for today. */
@Composable
fun AgendaScreen(
    date: LocalDate,
    today: LocalDate,
    events: List<EventEntity>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(date, events) {
        if (date != today) {
            listState.scrollToItem(0); return@LaunchedEffect
        }
        val nowMillis = System.currentTimeMillis()
        val timed = events.filterNot { it.isAllDay }.sortedBy { it.startMillis }
        val allDayCount = events.count { it.isAllDay }
        val upcomingIndex = timed.indexOfFirst { it.endMillis >= nowMillis }
        if (upcomingIndex >= 0) {
            listState.scrollToItem(allDayCount + upcomingIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        DateHeader(date = date, modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
        AgendaView(
            events = events,
            listState = listState,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
