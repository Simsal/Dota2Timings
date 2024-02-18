package de.octolearn.dota2timings

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class DotaTimingsApp : Application() {
    companion object {
        private lateinit var instance: DotaTimingsApp
        val database: AppDatabase by lazy {
            Room.databaseBuilder(instance,
                AppDatabase::class.java, "dota-timings")
                .addMigrations(MIGRATION_1_2).build()
        }

        // Migration defined within the companion to potentially allow easier access/testing
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE game_events ADD COLUMN inGameTime LONG NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE game_events ADD COLUMN timeBased BOOLEAN NOT NULL DEFAULT FALSE")
                database.execSQL("ALTER TABLE game_events ADD COLUMN eventBased BOOLEAN NOT NULL DEFAULT FALSE")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
