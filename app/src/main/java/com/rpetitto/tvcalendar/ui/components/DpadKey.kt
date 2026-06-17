package com.rpetitto.tvcalendar.ui.components

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * True if [event] is a KeyDown for any of the "select" / "enter" buttons on a
 * TV remote (DPAD_CENTER, Enter, NumpadEnter). Keeps key handling in screens tidy.
 */
fun KeyEvent.isSelectKeyDown(): Boolean =
    type == KeyEventType.KeyDown &&
        (key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter)
