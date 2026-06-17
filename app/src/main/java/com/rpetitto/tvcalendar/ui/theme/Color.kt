package com.rpetitto.tvcalendar.ui.theme

import androidx.compose.ui.graphics.Color

// Ambient base palette.
val AmbientBackground = Color(0xFF0A0A0F)
val AmbientSurface = Color(0xFF12121A)
val EventCardSurface = Color(0xFF1A1A24)
val CoolWhite = Color(0xFFEAEEF5)
val MutedGray = Color(0xFF8A8F9C)
val AccentBlue = Color(0xFF5B8DEF)
val Divider = Color(0xFF22222E)

/**
 * The ten standard Google Calendar event colors, keyed by the API's colorId
 * ("1".."11", with no "5" historically; we map the common set). Falls back to
 * [AccentBlue] for unknown ids.
 */
object GoogleCalendarColors {
    val Tomato = Color(0xFFD50000)
    val Flamingo = Color(0xFFE67C73)
    val Tangerine = Color(0xFFF4511E)
    val Banana = Color(0xFFF6BF26)
    val Sage = Color(0xFF33B679)
    val Basil = Color(0xFF0B8043)
    val Peacock = Color(0xFF039BE5)
    val Blueberry = Color(0xFF3F51B5)
    val Lavender = Color(0xFF7986CB)
    val Grape = Color(0xFF8E24AA)
    val Graphite = Color(0xFF616161)

    private val byColorId: Map<String, Color> = mapOf(
        "1" to Lavender,
        "2" to Sage,
        "3" to Grape,
        "4" to Flamingo,
        "5" to Banana,
        "6" to Tangerine,
        "7" to Peacock,
        "8" to Graphite,
        "9" to Blueberry,
        "10" to Basil,
        "11" to Tomato,
    )

    fun forColorId(colorId: String?): Color? = colorId?.let { byColorId[it] }

    /** Parses a "#rrggbb" hex string into a Color, or null if malformed. */
    fun parseHex(hex: String?): Color? {
        if (hex.isNullOrBlank()) return null
        return runCatching {
            val cleaned = hex.removePrefix("#")
            val value = cleaned.toLong(16)
            when (cleaned.length) {
                6 -> Color(0xFF000000 or value)
                8 -> Color(value)
                else -> null
            }
        }.getOrNull()
    }
}
