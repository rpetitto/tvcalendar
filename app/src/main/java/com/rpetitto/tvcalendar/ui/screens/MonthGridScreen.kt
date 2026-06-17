package com.rpetitto.tvcalendar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.ui.components.DayCell
import com.rpetitto.tvcalendar.ui.components.isSelectKeyDown
import com.rpetitto.tvcalendar.ui.theme.MutedGray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Classic 6-row × 7-col month grid. The focused day's containing month decides
 * which weeks are visible; days outside the focused month render dimmed.
 */
@Composable
fun MonthGridScreen(
    today: LocalDate,
    focusedDate: LocalDate,
    monthEvents: Map<LocalDate, List<EventEntity>>,
    onFocusDate: (LocalDate) -> Unit,
    onActivateDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusedMonth = focusedDate.month
    val focusedYear = focusedDate.year
    val firstOfMonth = focusedDate.withDayOfMonth(1)
    val gridStart = remember(firstOfMonth) {
        firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    }
    val days = remember(gridStart) { (0 until 42).map { gridStart.plusDays(it.toLong()) } }
    val requesters = remember { List(42) { FocusRequester() } }

    val focusIndex = days.indexOf(focusedDate).coerceAtLeast(0)
    LaunchedEffect(focusedDate) {
        runCatching { requesters[focusIndex].requestFocus() }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = firstOfMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = androidx.compose.ui.graphics.Color.White,
        )
        Spacer(Modifier.height(8.dp))
        WeekdayHeaderRow()
        Spacer(Modifier.height(4.dp))
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (row in 0 until 6) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (col in 0 until 7) {
                        val index = row * 7 + col
                        val day = days[index]
                        val interaction = remember(day) { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .focusRequester(requesters[index])
                                .onFocusChanged { if (it.isFocused) onFocusDate(day) }
                                .onPreviewKeyEvent { event ->
                                    if (event.isSelectKeyDown()) {
                                        onActivateDate(day); true
                                    } else false
                                }
                                .focusable(interactionSource = interaction)
                                .clickable(interactionSource = interaction, indication = null) {
                                    onActivateDate(day)
                                },
                        ) {
                            DayCell(
                                day = day,
                                isToday = day == today,
                                isFocused = day == focusedDate,
                                isInCurrentMonth = day.month == focusedMonth && day.year == focusedYear,
                                events = monthEvents[day].orEmpty(),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val sunday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        for (offset in 0 until 7) {
            val name = sunday.plusDays(offset.toLong()).dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MutedGray,
            )
        }
    }
}
