package com.rpetitto.tvcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.ui.theme.AccentBlue
import com.rpetitto.tvcalendar.ui.theme.AmbientSurface
import com.rpetitto.tvcalendar.ui.theme.CoolWhite
import com.rpetitto.tvcalendar.ui.theme.EventCardSurface
import com.rpetitto.tvcalendar.ui.theme.MutedGray
import java.time.LocalDate

/**
 * A focusable day cell: day-of-month number, optional event dots, with visual
 * states for "today" (filled accent circle) and "focused" (subtle outline).
 * Caller owns focus via Modifier.focusable / focusRequester / onKeyEvent.
 */
@Composable
fun DayCell(
    day: LocalDate,
    isToday: Boolean,
    isFocused: Boolean,
    isInCurrentMonth: Boolean,
    events: List<EventEntity>,
    modifier: Modifier = Modifier,
    showWeekday: Boolean = false,
) {
    val borderColor = if (isFocused) AccentBlue else Color.Transparent
    val containerColor = if (isFocused) AmbientSurface else Color.Transparent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            if (showWeekday) {
                Text(
                    text = day.dayOfWeek.getDisplayName(
                        java.time.format.TextStyle.SHORT,
                        java.util.Locale.getDefault(),
                    ).uppercase(),
                    fontSize = 11.sp,
                    color = if (isToday) CoolWhite else MutedGray,
                )
                Spacer(Modifier.height(4.dp))
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).then(
                    if (isToday) Modifier.background(AccentBlue) else Modifier
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    fontSize = 18.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isToday -> CoolWhite
                        !isInCurrentMonth -> MutedGray
                        else -> CoolWhite
                    },
                )
            }
            if (events.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                EventDots(events)
            }
        }
    }
}

/** Up to 4 colored dots representing the first events of the day. */
@Composable
private fun EventDots(events: List<EventEntity>) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        events.take(4).forEach { event ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(event.displayColor()),
            )
        }
        if (events.size > 4) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(EventCardSurface),
            )
        }
    }
}
