package de.octolearn.dota2timings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EventDao {
    @Insert
    suspend fun insertEvent(event: Event): Long // Return type can be Long for the ID of the inserted row

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<Event>

    @Query("SELECT * FROM events WHERE name = 'Game Paused' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPauseEvent(): Event?

    @Update
    suspend fun updateEvent(event: Event): Int // Return type can be Int indicating the number of rows updated
}

