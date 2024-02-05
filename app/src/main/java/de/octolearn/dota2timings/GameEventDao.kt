package de.octolearn.dota2timings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GameEventDao {
    @Insert
    suspend fun insertEvent(gameEvent: GameEvent): Long // Return type can be Long for the ID of the inserted row

    @Query("SELECT * FROM game_events")
    suspend fun getAllEvents(): List<GameEvent>

    @Query("SELECT * FROM events WHERE name = 'Game Paused' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPauseEvent(): GameEvent?

    @Update
    suspend fun updateEvent(gameEvent: GameEvent): Int // Return type can be Int indicating the number of rows updated
}

