package com.rpetitto.tvcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rpetitto.tvcalendar.R
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.ui.theme.CoolWhite
import com.rpetitto.tvcalendar.ui.theme.EventCardSurface
import com.rpetitto.tvcalendar.ui.theme.MutedGray
import com.rpetitto.tvcalendar.ui.theme.TvType

/**
 * A single event chip: a 4dp colored left border, title, time range (or
 * "All day"), and an optional location.
 */
@Composable
fun EventCard(event: EventEntity, modifier: Modifier = Modifier) {
    val accent = event.displayColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(EventCardSurface),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (event.location.isNullOrBlank()) 64.dp else 80.dp)
                .background(accent),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = event.title,
                style = TvType.eventTitle,
                color = CoolWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (event.isAllDay) stringResource(R.string.all_day) else event.timeRangeLabel(),
                style = TvType.eventTime,
                color = MutedGray,
            )
            if (!event.location.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = event.location,
                    style = TvType.eventLocation,
                    color = MutedGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
