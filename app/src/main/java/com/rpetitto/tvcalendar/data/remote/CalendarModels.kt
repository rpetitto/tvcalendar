package com.rpetitto.tvcalendar.data.remote

import com.google.gson.annotations.SerializedName

/** Response from GET /calendars/{id}/events. */
data class CalendarEventsResponse(
    @SerializedName("items") val items: List<CalendarEvent> = emptyList(),
)

data class CalendarEvent(
    @SerializedName("id") val id: String,
    @SerializedName("summary") val summary: String?,
    @SerializedName("description") val description: String? = null,
    @SerializedName("start") val start: EventDateTime,
    @SerializedName("end") val end: EventDateTime,
    @SerializedName("colorId") val colorId: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("htmlLink") val htmlLink: String? = null,
    @SerializedName("status") val status: String? = null,
)

/**
 * Calendar API represents timed events with [dateTime] (RFC3339) and all-day
 * events with [date] (yyyy-MM-dd). Exactly one is populated.
 */
data class EventDateTime(
    @SerializedName("dateTime") val dateTime: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("timeZone") val timeZone: String? = null,
)

/** Response from GET /users/me/calendarList. */
data class CalendarListResponse(
    @SerializedName("items") val items: List<CalendarListEntry> = emptyList(),
)

data class CalendarListEntry(
    @SerializedName("id") val id: String,
    @SerializedName("summary") val summary: String?,
    @SerializedName("backgroundColor") val backgroundColor: String? = null,
    @SerializedName("foregroundColor") val foregroundColor: String? = null,
    @SerializedName("primary") val primary: Boolean? = null,
    @SerializedName("selected") val selected: Boolean? = null,
)
