package de.octolearn.dota2timings

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_events")
data class GameEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: EventType,
    val inGameTime: Int, // Store time in seconds
    val timeBased: Boolean,
    val eventBased: Boolean,
    var remainingTime: Int? = null

)
