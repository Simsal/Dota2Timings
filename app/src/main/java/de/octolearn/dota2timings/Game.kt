package de.octolearn.dota2timings

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long // Store time in milliseconds

)
