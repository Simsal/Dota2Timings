package de.octolearn.dota2timings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GameEventDao {
    @Insert
    suspend fun insertEvent(gameEvent: GameEvent): Long // Return type can be Long for the ID of the inserted row

    @Query("SELECT * from game_events where id = :eventId")
    suspend fun getEventById(eventId: Int): GameEvent

    @Query("SELECT * FROM game_events")
    suspend fun getAllEvents(): List<GameEvent>

    @Query("SELECT * FROM game_events WHERE name = 'Game Paused' ORDER BY inGameTime DESC LIMIT 1")
    suspend fun getLastPauseEvent(): GameEvent?

    @Update
    suspend fun updateEvent(gameEvent: GameEvent): Int // Return type can be Int indicating the number of rows updated

    @Query("UPDATE game_events SET remainingTime = :remainingTime WHERE id = :eventId")
    suspend fun updateRemainingTime(eventId: Int, remainingTime: Long)
}

