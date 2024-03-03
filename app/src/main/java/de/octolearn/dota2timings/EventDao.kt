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

    @Query("SELECT * FROM events WHERE name = 'Game Started' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastStartEvent(): Event?

    // Select by name = "Game Started" and id =
    @Query("SELECT * FROM events WHERE name = 'Game Started' AND id = :id")
    suspend fun getStartEventById(id: Int): Event?

    @Query("SELECT SUM(timestamp) FROM events WHERE gameId = :gameId AND name = 'Game Paused'")
    suspend fun getPauseStartEventsSumForGame(gameId: Int): Long

    @Query("SELECT SUM(timestamp) FROM events WHERE gameId = :gameId AND name = 'Game resumed'")
    suspend fun getPauseEndEventsSumForGame(gameId: Int): Long

    @Update
    suspend fun updateEvent(event: Event): Int // Return type can be Int indicating the number of rows updated
}

