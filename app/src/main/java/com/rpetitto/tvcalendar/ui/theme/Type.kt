package com.rpetitto.tvcalendar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * TV-appropriate type scale — large enough to read from ~10 feet. Exposed both
 * as a Material [Typography] and as individual named tokens used directly by
 * the ambient widgets.
 */
object TvType {
    val clockLarge = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Thin)
    val dateLabel = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal)
    val dayHeader = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium)
    val eventTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)
    val eventTime = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val eventLocation = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
}

val AppTypography = Typography(
    displayLarge = TvType.clockLarge,
    headlineMedium = TvType.dayHeader,
    titleMedium = TvType.eventTitle,
    bodyMedium = TvType.eventTime,
    labelSmall = TvType.eventLocation,
)
