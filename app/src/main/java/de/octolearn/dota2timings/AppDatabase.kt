package de.octolearn.dota2timings

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Event::class, GameEvent::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun gameEventDao(): GameEventDao
}
