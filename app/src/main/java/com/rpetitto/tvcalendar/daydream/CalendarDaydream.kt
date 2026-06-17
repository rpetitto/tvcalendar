package com.rpetitto.tvcalendar.daydream

import android.service.dreams.DreamService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpetitto.tvcalendar.data.CalendarRepository
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.ui.components.displayColor
import com.rpetitto.tvcalendar.ui.components.timeRangeLabel
import com.rpetitto.tvcalendar.ui.theme.AccentBlue
import com.rpetitto.tvcalendar.ui.theme.AmbientBackground
import com.rpetitto.tvcalendar.ui.theme.CoolWhite
import com.rpetitto.tvcalendar.ui.theme.MutedGray
import com.rpetitto.tvcalendar.ui.theme.TVCalendarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Android system screensaver. Reads cached events out of Room (no network
 * required) and shows a slow clock plus today's next 3 events. Auto-exits on
 * any remote/touch input via the system's default dream behavior.
 */
class CalendarDaydream : DreamService() {

    private val eventsFlow = MutableStateFlow<List<EventEntity>>(emptyList())

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        setScreenBright(false)

        // Read cached events from Room; no API call.
        val repo = CalendarRepository.create(applicationContext)
        // Daydreams don't have a lifecycle scope; rely on Compose's LaunchedEffect.

        val composeView = ComposeView(this).apply {
            setContent {
                TVCalendarTheme {
                    val events by eventsFlow.collectAsState()
                    LaunchedEffect(Unit) {
                        repo.observeEventsForDay(LocalDate.now()).collect { eventsFlow.value = it }
                    }
                    DaydreamContent(events = events)
                }
            }
        }
        setContentView(composeView)
    }
}

@Composable
private fun DaydreamContent(events: List<EventEntity>) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(AmbientBackground).padding(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = now.format(DateTimeFormatter.ofPattern("h:mm")),
                fontSize = 168.sp,
                fontWeight = FontWeight.Thin,
                color = CoolWhite,
            )
            Text(
                text = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                fontSize = 28.sp,
                color = MutedGray,
            )
            Spacer(Modifier.height(48.dp))
            val nowMillis = System.currentTimeMillis()
            val upcoming = events
                .filterNot { it.isAllDay }
                .filter { it.endMillis >= nowMillis }
                .sortedBy { it.startMillis }
                .take(3)
            if (upcoming.isEmpty()) {
                Text(text = "Nothing left today", fontSize = 22.sp, color = MutedGray)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    upcoming.forEach { event -> UpcomingRow(event) }
                }
            }
        }
    }
}

@Composable
private fun UpcomingRow(event: EventEntity) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(event.displayColor()),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = event.title, fontSize = 22.sp, color = CoolWhite)
            Text(
                text = event.timeRangeLabel(),
                fontSize = 16.sp,
                color = AccentBlue,
            )
        }
    }
}
