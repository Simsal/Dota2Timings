package de.octolearn.dota2timings

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "dota_abilities")
data class DotaAbility(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val img: String,
    val cd: String,
    @ColumnInfo(name = "hero_name") val heroName: String
)
