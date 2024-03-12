package de.octolearn.dota2timings

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DotaAbilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(abilities: List<DotaAbility>)

    @Query("SELECT * FROM dota_abilities")
    fun getAllAbilities(): LiveData<List<DotaAbility>>

    @Query("SELECT DISTINCT hero_name FROM dota_abilities")
    fun getHeroes(): LiveData<List<String>>

    @Query("SELECT DISTINCT hero_name FROM dota_abilities")
    fun getAllHeroNames(): LiveData<List<String>>

    @Query("SELECT * FROM dota_abilities WHERE hero_name = :heroName")
    fun getAbilitiesForHero(heroName: String): LiveData<List<DotaAbility>>
}