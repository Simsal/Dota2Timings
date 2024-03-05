package de.octolearn.dota2timings

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

class DotaTimingsApp : Application() {
    companion object {
        private lateinit var instance: DotaTimingsApp
        val database: AppDatabase by lazy {
            Room.databaseBuilder(instance,
                AppDatabase::class.java, "dota-timings")
                .addCallback(roomCallback)
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

        private val roomCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Use Executors or another background thread method to run SQL insert statements
                Executors.newSingleThreadExecutor().execute {
                    // Populate the database in the background
                    // Example:
                    // db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Sanctuary', '/apps/dota2/images/dota_react/abilities/filler_ability.png', '240', 'Hero Name');")
                    // Add other insert statements...
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Fiend´s Grip', '/apps/dota2/images/dota_react/abilities/bane_fiends_grip.png', '120,110,100', 'Bane');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Echo Slam', '/apps/dota2/images/dota_react/abilities/earthshaker_echo_slam.png', '130,120,110', 'Earthshaker');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Omnislash', '/apps/dota2/images/dota_react/abilities/juggernaut_omni_slash.png', '120', 'Juggernaut');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Moonlight Shadow', '/apps/dota2/images/dota_react/abilities/mirana_invis.png', '140,120,100', 'Mirana');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Requiem of Souls', '/apps/dota2/images/dota_react/abilities/nevermore_requiem.png', '120,110,100', 'Shadow Fiend');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Morph', '/apps/dota2/images/dota_react/abilities/morphling_replicate.png', '140,100,60', 'Morphling');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Epicenter', '/apps/dota2/images/dota_react/abilities/sandking_epicenter.png', '120,110,100', 'Sandking');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('God´s Strength', '/apps/dota2/images/dota_react/abilities/sven_gods_strength.png', '110,105,100', 'Sven');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Thundergod´s Wrath', '/apps/dota2/images/dota_react/abilities/zuus_thundergods_wrath.png', '120', 'Zeus');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Finger of Death', '/apps/dota2/images/dota_react/abilities/lion_finger_of_death.png', '140,90,40', 'Lion');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Mass Serpent Ward', '/apps/dota2/images/dota_react/abilities/shadow_shaman_mass_serpent_ward.png', '110', 'Shadow Shaman');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Ravage', '/apps/dota2/images/dota_react/abilities/tidehunter_ravage.png', '150', 'Tidehunter');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Black Hole', '/apps/dota2/images/dota_react/abilities/enigma_black_hole.png', '180,170,160', 'Enigma');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Reaper´s Scythe', '/apps/dota2/images/dota_react/abilities/necrolyte_reapers_scythe.png', '110', 'Necrolyte');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Chaotic Offering', '/apps/dota2/images/dota_react/abilities/warlock_rain_of_chaos.png', '160', 'Warlock');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Sonic Wave', '/apps/dota2/images/dota_react/abilities/queenofpain_sonic_wave.png', '110,95,80', 'Queen of pain');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Chronosphere', '/apps/dota2/images/dota_react/abilities/faceless_void_chronosphere.png', '160,150,140', 'Faceless Void');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Reincarnation', '/apps/dota2/images/dota_react/abilities/skeleton_king_reincarnation.png', '180,150,120', 'Skeleton King');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Exorcism', '/apps/dota2/images/dota_react/abilities/death_prophet_exorcism.png', '150', 'Death Prophet');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Eclipse', '/apps/dota2/images/dota_react/abilities/luna_eclipse.png', '110', 'Luna');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Dark Ascension', '/apps/dota2/images/dota_react/abilities/night_stalker_darkness.png', '140,135,130', 'Night Stalker');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Hand of God', '/apps/dota2/images/dota_react/abilities/chen_hand_of_god.png', '150,130,110', 'Chen');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Haunt', '/apps/dota2/images/dota_react/abilities/spectre_haunt.png', '160', 'Spectre');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Doom', '/apps/dota2/images/dota_react/abilities/doom_bringer_doom.png', '140,130,120', 'Doom');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Global Silence', '/apps/dota2/images/dota_react/abilities/silencer_global_silence.png', '130,115,100', 'Silencer');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Sanity´s Eclipse', '/apps/dota2/images/dota_react/abilities/obsidian_destroyer_sanity_eclipse.png', '160,145,130', 'Obsidian Destroyer');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Wolf Bite', '/apps/dota2/images/dota_react/abilities/lycan_wolf_bite.png', '125,110,95', 'Lycan');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Shapeshift', '/apps/dota2/images/dota_react/abilities/lycan_shapeshift.png', '110,100,90', 'Lycan');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Primal Split', '/apps/dota2/images/dota_react/abilities/brewmaster_primal_split.png', '140,130,120', 'Brewmaster');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Summon Spirit Bear', '/apps/dota2/images/dota_react/abilities/lone_druid_spirit_bear.png', '150,140,130,120', 'Lone Druid');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Overgrowth', '/apps/dota2/images/dota_react/abilities/treant_overgrowth.png', '120,110,100', 'Treant Protector');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Flesh Golem', '/apps/dota2/images/dota_react/abilities/undying_flesh_golem.png', '140', 'Undying');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Song of the Siren', '/apps/dota2/images/dota_react/abilities/naga_siren_song_of_the_siren.png', '180,130,80', 'Naga Siren');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Summon Familiars', '/apps/dota2/images/dota_react/abilities/visage_summon_familiars.png', '130,120,110', 'Visage');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Reverse Polarity', '/apps/dota2/images/dota_react/abilities/magnataur_reverse_polarity.png', '120', 'Magnus');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Metamorphosis', '/apps/dota2/images/dota_react/abilities/terrorblade_metamorphosis.png', '150', 'Terrorblade');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Sunder', '/apps/dota2/images/dota_react/abilities/terrorblade_sunder.png', '120,80,40', 'Terrorblade');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Supernova', '/apps/dota2/images/dota_react/abilities/phoenix_supernova.png', '120', 'Phoenix');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('False Promise', '/apps/dota2/images/dota_react/abilities/oracle_false_promise.png', '110,85,60', 'Oracle');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Fiend´s Gate', '/apps/dota2/images/dota_react/abilities/abyssal_underlord_dark_portal.png', '110,100,90', 'Underlord');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Mortimer Kisses', '/apps/dota2/images/dota_react/abilities/snapfire_mortimer_kisses.png', '120,110,100', 'Snapfire');")
                    db.execSQL("INSERT INTO dota_abilities (dname, img, cd, hero_name) VALUES ('Solar Guardian', '/apps/dota2/images/dota_react/abilities/dawnbreaker_solar_guardian.png', '120,105,90', 'Dawnbreaker');")

                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
