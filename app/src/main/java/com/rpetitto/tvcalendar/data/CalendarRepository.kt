package com.rpetitto.tvcalendar.data

import android.content.Context
import android.util.Log
import com.rpetitto.tvcalendar.BuildConfig
import com.rpetitto.tvcalendar.auth.TokenRefresher
import com.rpetitto.tvcalendar.auth.TokenStore
import com.rpetitto.tvcalendar.data.local.CalendarDatabase
import com.rpetitto.tvcalendar.data.local.EventDao
import com.rpetitto.tvcalendar.data.local.EventEntity
import com.rpetitto.tvcalendar.data.remote.CalendarApiService
import com.rpetitto.tvcalendar.data.remote.CalendarEvent
import com.rpetitto.tvcalendar.data.remote.RetrofitModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Single source of truth for calendar data. Fetches from the Calendar API,
 * caches into Room, and exposes reactive [Flow]s the UI observes. The UI never
 * talks to the API directly.
 */
class CalendarRepository(
    private val api: CalendarApiService,
    private val eventDao: EventDao,
    private val tokenStore: TokenStore,
    private val tokenRefresher: TokenRefresher,
    private val clientId: String = BuildConfig.GOOGLE_CLIENT_ID,
    private val clientSecret: String = BuildConfig.GOOGLE_CLIENT_SECRET,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {

    /** Explicit, non-nullable outcome for a sync attempt. */
    sealed interface SyncResult {
        data object Success : SyncResult
        data object NeedsAuth : SyncResult
        data class NetworkError(val cause: Throwable) : SyncResult
        data class Error(val message: String) : SyncResult
    }

    /**
     * Refreshes the token if needed, fetches the next 14 days of events across
     * all calendars, writes them to Room, and prunes stale rows.
     */
    suspend fun syncEvents(): SyncResult {
        val token = ensureAccessToken() ?: return SyncResult.NeedsAuth
        val auth = "Bearer $token"

        return try {
            val now = OffsetDateTime.now(zoneId)
            val timeMin = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val timeMax = now.plusDays(SYNC_WINDOW_DAYS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            // Pull every calendar the user can read so multi-calendar setups work.
            val calendars = runCatching { api.getCalendarList(auth).items }
                .getOrElse { emptyList() }
            val colorById = calendars.associate { it.id to it.backgroundColor }
            val calendarIds = calendars.map { it.id }.ifEmpty { listOf("primary") }

            val entities = mutableListOf<EventEntity>()
            for (calId in calendarIds) {
                val items = api.getEvents(
                    auth = auth,
                    calendarId = calId,
                    timeMin = timeMin,
                    timeMax = timeMax,
                ).items
                items.mapNotNullTo(entities) { event ->
                    event.toEntity(calId, colorById[calId])
                }
            }

            eventDao.upsertEvents(entities)
            // Keep yesterday so events that started before "now" still show today.
            val yesterdayStart = LocalDate.now(zoneId).minusDays(1)
                .atStartOfDay(zoneId).toInstant().toEpochMilli()
            eventDao.deleteEventsOlderThan(yesterdayStart)

            SyncResult.Success
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                Log.w(TAG, "401 from Calendar API — refresh token likely invalid")
                SyncResult.NeedsAuth
            } else {
                SyncResult.Error("HTTP ${e.code()}")
            }
        } catch (e: java.io.IOException) {
            Log.w(TAG, "Network error during sync", e)
            SyncResult.NetworkError(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected sync error", e)
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    /** Returns a valid access token, refreshing silently if needed, or null. */
    private suspend fun ensureAccessToken(): String? {
        if (tokenStore.isTokenValid()) return tokenStore.getAccessToken()
        val refreshToken = tokenStore.getRefreshToken() ?: return null
        return tokenRefresher.refresh(clientId, clientSecret, refreshToken, tokenStore)
    }

    fun observeEventsForDay(date: LocalDate): Flow<List<EventEntity>> {
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return eventDao.getEventsInRange(start, end)
    }

    fun observeEventsForWeek(weekStart: LocalDate): Flow<Map<LocalDate, List<EventEntity>>> =
        observeEventsByDay(weekStart, 7)

    /**
     * Observes events for a calendar month's visible grid: always 42 days (6
     * weeks), starting on the Sunday on or before the 1st of the month.
     */
    fun observeEventsForMonth(month: LocalDate): Flow<Map<LocalDate, List<EventEntity>>> {
        val firstOfMonth = month.withDayOfMonth(1)
        val gridStart = firstOfMonth.with(java.time.temporal.TemporalAdjusters
            .previousOrSame(java.time.DayOfWeek.SUNDAY))
        return observeEventsByDay(gridStart, 42)
    }

    private fun observeEventsByDay(
        startDay: LocalDate,
        dayCount: Int,
    ): Flow<Map<LocalDate, List<EventEntity>>> {
        val start = startDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = startDay.plusDays(dayCount.toLong()).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return eventDao.getEventsInRange(start, end).map { events ->
            val days = (0 until dayCount).associate { offset ->
                startDay.plusDays(offset.toLong()) to mutableListOf<EventEntity>()
            }
            for (event in events) {
                // An event can span multiple days; add it to each day it touches.
                val eventStart = Instant.ofEpochMilli(event.startMillis).atZone(zoneId).toLocalDate()
                val eventEnd = Instant.ofEpochMilli(event.endMillis - 1).atZone(zoneId).toLocalDate()
                days.keys.forEach { day ->
                    if (!day.isBefore(eventStart) && !day.isAfter(eventEnd)) {
                        days[day]?.add(event)
                    }
                }
            }
            days.mapValues { it.value.toList() }
        }
    }

    /**
     * Emits [Unit] once at every local midnight so the UI can roll over to the
     * new day. Emits immediately on collection is intentionally avoided.
     */
    fun midnightTicks(): Flow<Unit> = flow {
        while (true) {
            val now = OffsetDateTime.now(zoneId)
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zoneId)
            val millisUntil = Duration.between(now.toInstant(), nextMidnight.toInstant()).toMillis()
            kotlinx.coroutines.delay(millisUntil.coerceAtLeast(1000))
            emit(Unit)
        }
    }

    // --- mapping -----------------------------------------------------------

    private fun CalendarEvent.toEntity(calendarId: String, calendarColor: String?): EventEntity? {
        if (status == "cancelled") return null
        val (startMillis, isAllDay) = parseStart() ?: return null
        val endMillis = parseEnd(isAllDay) ?: (startMillis + Duration.ofHours(1).toMillis())
        return EventEntity(
            id = "$calendarId:$id",
            calendarId = calendarId,
            title = summary ?: "(No title)",
            description = description,
            location = location,
            colorId = colorId,
            calendarColor = calendarColor,
            isAllDay = isAllDay,
            startMillis = startMillis,
            endMillis = endMillis,
            htmlLink = htmlLink,
        )
    }

    /** Returns (epochMillis, isAllDay) or null if unparseable. */
    private fun CalendarEvent.parseStart(): Pair<Long, Boolean>? {
        start.dateTime?.let { return parseRfc3339(it)?.let { ms -> ms to false } }
        start.date?.let { date ->
            return runCatching {
                LocalDate.parse(date).atStartOfDay(zoneId).toInstant().toEpochMilli() to true
            }.getOrNull()
        }
        return null
    }

    private fun CalendarEvent.parseEnd(isAllDay: Boolean): Long? {
        end.dateTime?.let { return parseRfc3339(it) }
        end.date?.let { date ->
            // All-day end date is exclusive; subtract a tick so it lands on the last day.
            return runCatching {
                LocalDate.parse(date).atStartOfDay(zoneId).toInstant().toEpochMilli()
            }.getOrNull()
        }
        return null
    }

    private fun parseRfc3339(value: String): Long? = runCatching {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    }.recoverCatching {
        // Fall back for values without an offset.
        LocalDate.parse(value.substring(0, 10)).atTime(LocalTime.NOON)
            .atZone(zoneId).toInstant().toEpochMilli()
    }.getOrNull()

    companion object {
        private const val TAG = "CalendarRepository"
        // Cover today + 6 weeks so the Month view's grid always has data.
        private const val SYNC_WINDOW_DAYS = 45L

        /** Builds a repository with default wiring from an app [Context]. */
        fun create(context: Context): CalendarRepository {
            val db = CalendarDatabase.getInstance(context)
            return CalendarRepository(
                api = RetrofitModule.provideCalendarApiService(),
                eventDao = db.eventDao(),
                tokenStore = TokenStore(context),
                tokenRefresher = TokenRefresher(),
            )
        }
    }
}
