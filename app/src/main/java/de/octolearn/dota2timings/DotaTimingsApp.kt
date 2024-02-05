package de.octolearn.dota2timings

import android.app.Application
import androidx.room.Room

class DotaTimingsApp : Application() {
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this,
            AppDatabase::class.java, "dota-timings").build()
    }
}