package com.rpetitto.tvcalendar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A cached calendar event. [startMillis]/[endMillis] are denormalized epoch
 * millis so the DAO can do efficient range queries without parsing RFC3339.
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val calendarId: String,
    val title: String,
    val description: String?,
    val location: String?,
    val colorId: String?,
    /** Calendar's own background color hex (e.g. "#039be5"), if known. */
    val calendarColor: String?,
    val isAllDay: Boolean,
    val startMillis: Long,
    val endMillis: Long,
    val htmlLink: String?,
)
