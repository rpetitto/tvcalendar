package com.rpetitto.tvcalendar.ui.components

import androidx.compose.ui.graphics.Color
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.ui.theme.AccentBlue
import com.rpetitto.tvcalendar.ui.theme.GoogleCalendarColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm")
private val TIME_FMT_AMPM = DateTimeFormatter.ofPattern("h:mm a")

/** Resolves the display color for an event: per-event colorId, then calendar color, then accent. */
fun EventEntity.displayColor(): Color =
    GoogleCalendarColors.forColorId(colorId)
        ?: GoogleCalendarColors.parseHex(calendarColor)
        ?: AccentBlue

/**
 * Human-readable time range, e.g. "2:30 – 3:00 PM". Drops the AM/PM on the
 * start when it matches the end to reduce clutter.
 */
fun EventEntity.timeRangeLabel(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val start = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalTime()
    val end = Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalTime()
    val startAmPm = if (start.hour < 12) "AM" else "PM"
    val endAmPm = if (end.hour < 12) "AM" else "PM"
    val startStr = if (startAmPm == endAmPm) start.format(TIME_FMT) else start.format(TIME_FMT_AMPM)
    return "$startStr – ${end.format(TIME_FMT_AMPM)}"
}
