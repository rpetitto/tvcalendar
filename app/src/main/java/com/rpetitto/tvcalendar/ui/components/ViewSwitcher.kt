package com.rpetitto.tvcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpetitto.tvcalendar.ui.CalendarView
import com.rpetitto.tvcalendar.ui.theme.AccentBlue
import com.rpetitto.tvcalendar.ui.theme.AmbientSurface
import com.rpetitto.tvcalendar.ui.theme.CoolWhite
import com.rpetitto.tvcalendar.ui.theme.MutedGray

/** Top tab bar showing Week / Month / Agenda; current view is highlighted. */
@Composable
fun ViewSwitcher(
    current: CalendarView,
    onSelect: (CalendarView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (view in CalendarView.values()) {
            ViewTab(
                label = view.name,
                isCurrent = view == current,
                onClick = { onSelect(view) },
            )
        }
    }
}

@Composable
private fun ViewTab(label: String, isCurrent: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val container = when {
        isCurrent -> AccentBlue
        isFocused -> AmbientSurface
        else -> Color.Transparent
    }
    val border = if (isFocused && !isCurrent) AccentBlue else Color.Transparent

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .border(2.dp, border, RoundedCornerShape(6.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) CoolWhite else MutedGray,
        )
    }
}
