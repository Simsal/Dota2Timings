package de.octolearn.dota2timings

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Event::class, GameEvent::class, Game::class, DotaAbility::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun gameEventDao(): GameEventDao
    abstract fun gameDao(): GameDao
    abstract fun dotaAbilityDao(): DotaAbilityDao
}
