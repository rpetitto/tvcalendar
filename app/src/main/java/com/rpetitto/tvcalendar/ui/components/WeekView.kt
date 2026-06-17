package com.rpetitto.tvcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.ui.theme.AccentBlue
import com.rpetitto.tvcalendar.ui.theme.CoolWhite
import com.rpetitto.tvcalendar.ui.theme.MutedGray
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Fixed-width (80dp) sidebar showing the seven days of [weekStart]'s week. The
 * current day gets a filled accent circle behind its date; days with events get
 * a small colored dot.
 */
@Composable
fun WeekView(
    weekStart: LocalDate,
    today: LocalDate,
    eventsByDay: Map<LocalDate, List<EventEntity>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (offset in 0 until 7) {
            val day = weekStart.plusDays(offset.toLong())
            val events = eventsByDay[day].orEmpty()
            DayChip(
                day = day,
                isToday = day == today,
                dotColor = events.firstOrNull()?.displayColor(),
            )
        }
    }
}

@Composable
private fun DayChip(day: LocalDate, isToday: Boolean, dotColor: Color?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
            fontSize = 12.sp,
            color = if (isToday) CoolWhite else MutedGray,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(if (isToday) Modifier.background(AccentBlue) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                fontSize = 20.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = CoolWhite,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .then(if (dotColor != null) Modifier.background(dotColor) else Modifier),
        )
    }
}
