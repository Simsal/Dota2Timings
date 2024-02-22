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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
        }

        // Migration defined within the companion to potentially allow easier access/testing
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_events ADD COLUMN inGameTime LONG NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE game_events ADD COLUMN timeBased INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE game_events ADD COLUMN eventBased INTEGER NOT NULL DEFAULT 0")

            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            CREATE TABLE games (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                startTime INTEGER NOT NULL                
            )
        """.trimIndent())
            }
        }


        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN gameId INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
