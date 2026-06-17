package com.rpetitto.tvcalendar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AmbientColorScheme = darkColorScheme(
    background = AmbientBackground,
    surface = AmbientSurface,
    onBackground = CoolWhite,
    onSurface = CoolWhite,
    primary = AccentBlue,
    onPrimary = CoolWhite,
    surfaceVariant = EventCardSurface,
    onSurfaceVariant = MutedGray,
)

/** Dark, ambient theme for the always-on TV display. */
@Composable
fun TVCalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AmbientColorScheme,
        typography = AppTypography,
        content = content,
    )
}
