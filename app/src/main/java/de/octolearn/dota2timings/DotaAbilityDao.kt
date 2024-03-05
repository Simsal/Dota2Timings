package de.octolearn.dota2timings

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DotaAbilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(abilities: List<DotaAbility>)

    @Query("SELECT * FROM dota_abilities")
    fun getAllAbilities(): LiveData<List<DotaAbility>>
}