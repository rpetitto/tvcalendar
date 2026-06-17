package com.rpetitto.tvcalendar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rpetitto.tvcalendar.R
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.ui.theme.MutedGray
import com.rpetitto.tvcalendar.ui.theme.TvType

/**
 * Scrolling agenda for one day. All-day events float to the top; timed events
 * follow chronologically with a footer once the list ends.
 */
@Composable
fun AgendaView(
    events: List<EventEntity>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val allDay = events.filter { it.isAllDay }
    val timed = events.filterNot { it.isAllDay }.sortedBy { it.startMillis }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(allDay, key = { "allday-${it.id}" }) { event ->
            EventCard(event)
        }
        items(timed, key = { it.id }) { event ->
            EventCard(event)
        }
        item {
            Text(
                text = stringResource(
                    if (events.isEmpty()) R.string.agenda_no_events else R.string.agenda_empty
                ),
                style = TvType.eventTime,
                color = MutedGray,
                modifier = Modifier.padding(top = 12.dp, start = 4.dp),
            )
        }
    }
}
