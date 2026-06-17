package com.rpetitto.tvcalendar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    /** Events overlapping [startMillis, endMillis), ordered chronologically. */
    @Query(
        """
        SELECT * FROM events
        WHERE startMillis < :endMillis AND endMillis > :startMillis
        ORDER BY isAllDay DESC, startMillis ASC
        """
    )
    fun getEventsInRange(startMillis: Long, endMillis: Long): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE endMillis < :millis")
    suspend fun deleteEventsOlderThan(millis: Long)

    @Query("DELETE FROM events")
    suspend fun clearAll()
}
