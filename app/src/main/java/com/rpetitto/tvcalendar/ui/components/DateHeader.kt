package com.rpetitto.tvcalendar.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rpetitto.tvcalendar.ui.theme.CoolWhite
import com.rpetitto.tvcalendar.ui.theme.MutedGray
import com.rpetitto.tvcalendar.ui.theme.TvType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DAY_FMT = DateTimeFormatter.ofPattern("EEEE")
private val MONTH_FMT = DateTimeFormatter.ofPattern("MMMM d")

/** Day-of-week heading over a muted month/day line, used above the agenda. */
@Composable
fun DateHeader(date: LocalDate, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = date.format(DAY_FMT), style = TvType.dayHeader, color = CoolWhite)
        Text(text = date.format(MONTH_FMT), style = TvType.eventTime, color = MutedGray)
    }
}
