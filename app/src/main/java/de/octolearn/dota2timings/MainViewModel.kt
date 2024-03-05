package de.octolearn.dota2timings

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.NoLiveLiterals
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
@NoLiveLiterals
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database: AppDatabase = DotaTimingsApp.database

    private val eventDao = database.eventDao()
    private val gameEventDao = database.gameEventDao()
    private val gameDao = database.gameDao()

    // current game id
    var currentGameId: Int = 0

    // LiveData to hold your events
    val events = MutableLiveData<List<EventType>>(listOf())
    var gameState = MutableLiveData(GameState.NOT_STARTED)
    val occurredGameEvents = MutableLiveData<List<FrontendGameEvent>>()

    // LiveData to track if the "Roshan Killed" and "Tormentor" action is enabled
    val isRoshanActionEnabled = MutableLiveData<Boolean>(true)
    val isDireTormentorActionEnabled = MutableLiveData<Boolean>(false)
    val isRadiantTormentorActionEnabled = MutableLiveData<Boolean>(false)

    // LiveData to track how many roshan kills have occurred
    val roshanKills = MutableLiveData(0)

    // LiveData if aegis is active
    enum class RoshanState {
        ALIVE, KILLED, AEGIS_DISAPPEARED, COULD_RESPAWN
    }
    val roshanState = MutableLiveData<RoshanState>(RoshanState.ALIVE)


    // LiveData if it is night
    val isNight = MutableLiveData(true)

    // A data class to hold timer job and elapsed time information
    data class TimerInfo(val job: Job, val startTime: Long, val inGameTime: Int)

    // A data class to store text + timestamp
    data class FrontendGameEvent(
        val message: String,
        val iconIds: List<String>,
        val timestamp: String // In-game time as a string
    )

    // Map to store timer info for each event
    private val eventTimers = mutableMapOf<Int, TimerInfo>()


    private var gameTimerJob: Job? = null
    private var pauseTimerJob: Job? = null

    private var minRespawnJob: Job? = null
    private var maxRespawnJob: Job? = null
    private var aegisDespawnJob: Job? = null

    private var pauseStartTime: Long = 0L
    var gameTime = MutableLiveData("-01:30")
    var pauseTime = MutableLiveData("00:00")
    var gameTimeInSeconds = -90   // Starting time (-01:30) in seconds
    var pauseTimeInSeconds = 0
    var pausedAtSecond: Int? = null

    //dev
    private var delay = 100L
    //prod
    //private var delay = 1000L

    enum class GameState {
        NOT_STARTED, RUNNING, PAUSED, ENDED
    }

    @NoLiveLiterals
    fun startGame() {
        gameTimerJob?.cancel() // Cancel any existing job



        if (gameState.value == GameState.NOT_STARTED) {
            // Insert game in the database
            viewModelScope.launch {
                // Insert game in the database and await the new game ID
                val game = Game(startTime = System.currentTimeMillis())
                currentGameId = gameDao.insertGame(game).toInt()

                // Now that currentGameId is set, insert the game start event into the database
                val startEvent = Event(name = "Game Started", timestamp = System.currentTimeMillis(), gameId = currentGameId)
                eventDao.insertEvent(startEvent)

                // With the database operations complete, proceed with starting the game
                startGameTimer()
                onEventTriggered(EventType.GAME_STARTED, gameTimeInSeconds, listOf("dire_optimized", "radiant_optimized"))
                gameState.value = GameState.RUNNING
                Log.i("MainViewModel", "Game started")
            }

        } else if (gameState.value == GameState.RUNNING) {
            // Pause the game
            gameState.value = GameState.PAUSED
            onEventTriggered(EventType.GAME_PAUSED, gameTimeInSeconds, listOf( "dire_optimized", "radiant_optimized" ))

            Log.i("MainViewModel", "Game paused")

            pauseGame()

        } else if (gameState.value == GameState.PAUSED) {
            // Resume the game
            gameState.value = GameState.RUNNING
            resumeGameTimer()
            onEventTriggered(EventType.GAME_RESUMED, gameTimeInSeconds, listOf( "dire_optimized", "radiant_optimized" ))
            Log.i("MainViewModel", "Game resumed")
            stopPauseTimer()

        }
    }

    fun endGame() {
        gameTimerJob?.cancel() // Stop the game timer
        pauseTimerJob?.cancel() // Stop the pause timer if running
        gameState.value = GameState.ENDED

        // clear roshan state
        roshanState.value = RoshanState.ALIVE
        // clear roshan kills
        roshanKills.value = 0

        // Insert game end event into the database
        viewModelScope.launch {
            val endEvent = Event(name = "Game Ended", timestamp = System.currentTimeMillis(), gameId = currentGameId)
            eventDao.insertEvent(endEvent)
        }
        // clear all timers
        eventTimers.forEach { (_, timerInfo) -> timerInfo.job.cancel() }
        eventTimers.clear()

        // reset game time
        gameTimeInSeconds = -90
        gameTime.value = formatTime(gameTimeInSeconds)

        // reset pause time
        pauseTimeInSeconds = 0

        // reset current game id
        currentGameId = 0

        // reset game state
        gameState.value = GameState.NOT_STARTED

        // reset occurred game events
        occurredGameEvents.value = emptyList()



        Log.i("MainViewModel", "Game ended")
    }

    private fun triggerTimedEvents(gameTimeInSeconds: Int) {
        // Day Night Cycle, switches every 5 minutes
        if (gameTimeInSeconds % 300 == 0) {
            isNight.value = !isNight.value!!
            if (isNight.value == true) {
                onEventTriggered(EventType.NIGHT_CYCLE, gameTimeInSeconds, listOf("night_optimized"))
            } else {
                onEventTriggered(EventType.DAY_CYCLE, gameTimeInSeconds, listOf("day_optimized"))
            }
        }

        // Bounty Rune: spawns every 3 minutes (180 seconds), starting at 0 seconds
        if (gameTimeInSeconds % 180 == 0) {
            onEventTriggered(EventType.BOUNTY_RUNE, gameTimeInSeconds, List(1) { "bounty" })
        }
        // Power Rune: spawns every 2 minutes (120 seconds), starting at 6 minutes (360 seconds)
        if (gameTimeInSeconds >= 360 && (gameTimeInSeconds - 360) % 120 == 0) {

            // add all these runes "haste", "illusion", "invisibility", "regeneration", "amplify_damage", "arcane", "shield"
            onEventTriggered(EventType.POWER_RUNE, gameTimeInSeconds, listOf( "haste_optimized", "illusion_optimized", "invisibility_optimized", "regen_optimized", "amplify_damage_optimized", "arcane_optimized", "shield_optimized"))
        }

        // Wisdom Rune: spawns every 7 minutes (420 seconds), starting at 7 minutes (420 seconds)
        if (gameTimeInSeconds >= 420 && (gameTimeInSeconds - 420) % 420 == 0) {
            onEventTriggered(EventType.WISDOM_RUNE, gameTimeInSeconds, List(1) { "wisdom_optimized" })
        }

        // Lotus: spawns every 3 minutes (180 seconds), starting at 3 minutes (180 seconds)
        if (gameTimeInSeconds >= 180 && (gameTimeInSeconds - 180) % 180 == 0) {
            onEventTriggered(EventType.LOTUS, gameTimeInSeconds, List(1) { "lotus_optimized" })
        }

        // Tormentor: spawns at 20 minutes (1200 seconds)
        if (gameTimeInSeconds == 1200) {
            onEventTriggered(EventType.TORMENTOR, gameTimeInSeconds, listOf("dire_tormentor_optimized", "radiant_tormentor_optimized" ))
            isDireTormentorActionEnabled.value = true
            isRadiantTormentorActionEnabled.value = true
        }

        // Water Rune: spawns at 2 minutes (120 seconds) and 4 minutes (240 seconds)
        if (gameTimeInSeconds == 120 || gameTimeInSeconds == 240) {
            onEventTriggered(EventType.WATER_RUNE, gameTimeInSeconds, List(1) { "water_optimized" })
        }

        // Neutral items tier 1: spawn at 7 minutes
        if (gameTimeInSeconds == 420) {
            onEventTriggered(EventType.NEUTRAL_ITEM_TIER_1, gameTimeInSeconds, List(1) {"tier_1_optimized"})
        }

        // Neutral items tier 2: spawn at 17 minutes
        if (gameTimeInSeconds == 1020) {
            onEventTriggered(EventType.NEUTRAL_ITEM_TIER_2, gameTimeInSeconds, List(1) {"tier_2_optimized"})
        }

        // Neutral items tier 3: spawn at 27 minutes
        if (gameTimeInSeconds == 1620) {
            onEventTriggered(EventType.NEUTRAL_ITEM_TIER_3, gameTimeInSeconds, List(1) {"tier_3_optimized"})
        }

        // Neutral items tier 4: spawn at 37 minutes
        if (gameTimeInSeconds == 2220) {
            onEventTriggered(EventType.NEUTRAL_ITEM_TIER_4, gameTimeInSeconds, List(1) {"tier_4_optimized"})
        }

        // Neutral items tier 5: spawn at 60 minutes
        if (gameTimeInSeconds == 3600) {
            onEventTriggered(EventType.NEUTRAL_ITEM_TIER_5, gameTimeInSeconds, List(1) {"tier_5_optimized"})
        }

    }

    private fun startGameTimer() {
        gameTimerJob = viewModelScope.launch {
            while (true) {
                gameTime.value = formatTime(gameTimeInSeconds)
                // Trigger events based on gameTimeInSeconds
                triggerTimedEvents(gameTimeInSeconds)
                delay(delay) // Delay for 1 second

                gameTimeInSeconds++
            }
        }
    }


    private fun adjustGameTime() {
        Log.i("MainViewModel", "Adjusting game time")
        viewModelScope.launch {
            val gameStartedEvent = eventDao.getStartEventById(currentGameId) ?: return@launch

            // Fetch all pause started and ended events for the current game
            val pauseStartEventsSum = eventDao.getPauseStartEventsSumForGame(currentGameId)
            val pauseEndEventsSum = eventDao.getPauseEndEventsSumForGame(currentGameId)

            // Calculate total pause duration in seconds
            val totalPauseDuration = (pauseEndEventsSum - pauseStartEventsSum) / 1000

            val gameStartedTime = gameStartedEvent.timestamp
            val currentTime = System.currentTimeMillis()
            // Adjust time passed since game started by subtracting total pause duration
            val timePassedSinceGameStarted = (currentTime - gameStartedTime) / 1000 - totalPauseDuration + 90

            val incrementsNeeded = timePassedSinceGameStarted - gameTimeInSeconds

            Log.i("MainViewModel", "Increments needed: $incrementsNeeded")

            repeat(incrementsNeeded.toInt()) {
                gameTimeInSeconds++
                gameTime.postValue(formatTime(gameTimeInSeconds))
                triggerTimedEvents(gameTimeInSeconds)
                delay(1) // Minimal delay to prevent freezing, adjust as needed
            }
        }
    }


    fun pauseGame() {
        gameTimerJob?.cancel() // Stop the game timer
        pausedAtSecond = gameTimeInSeconds // Store the game time at pause
        val pauseStartTime = System.currentTimeMillis() // Record the pause start time

        viewModelScope.launch {
            val pauseEvent = Event(name = "Game Paused", timestamp = pauseStartTime, gameId = currentGameId)
            eventDao.insertEvent(pauseEvent)
        }

        startPauseTimer() // Optionally start a visual pause timer, if this is different from tracking pause duration

        eventTimers.forEach { (eventId, timerInfo) ->
            timerInfo.job.cancel()
            val elapsedTimeMs = System.currentTimeMillis() - timerInfo.startTime
            val elapsedTimeSec = elapsedTimeMs / 1000
            val remainingTime = max(0, timerInfo.inGameTime - elapsedTimeSec) // already in seconds

            viewModelScope.launch {
                gameEventDao.updateRemainingTime(eventId, remainingTime)
            }
        }
        gameState.value = GameState.PAUSED
    }

    fun startPauseTimer() {
        pauseTimerJob?.cancel() // Cancel any existing pause timer

        pauseTimerJob = viewModelScope.launch {
            while (true) {
                pauseTime.value = formatTime(pauseTimeInSeconds)
                delay(delay)
                pauseTimeInSeconds++
            }
        }
    }

    fun stopPauseTimer() {
        pauseTimerJob?.cancel()
        pauseTime.value = "00:00"
        // Optionally save the pause duration to a database
    }

    private fun resumeGameTimer() {

        val pauseEndTime = System.currentTimeMillis()
        val pauseDurationSeconds = (pauseEndTime - pauseStartTime) / 1000

        viewModelScope.launch {
            // Log the resume event in the database
            val resumeEvent = Event(name = "Game Resumed", timestamp = pauseEndTime, gameId = currentGameId)
            eventDao.insertEvent(resumeEvent)

            // Recalculate remaining times for scheduled events if necessary
            eventTimers.forEach { (eventId, timerInfo) ->
                val adjustedRemainingTime = timerInfo.inGameTime - gameTimeInSeconds
                if (adjustedRemainingTime > 0) {
                    // Reschedule the event with the adjusted remaining time
                    rescheduleEvent(eventId, adjustedRemainingTime)
                }
            }
        }

        adjustGameTime()
        startGameTimer()
    }

    private fun rescheduleEvent(eventId: Int, remainingTime: Int) {
        // Cancel the existing timer for the event
        eventTimers[eventId]?.job?.cancel()

        // Assuming remainingTime is in seconds, convert to milliseconds for delay
        val delayTimeMs = remainingTime * delay

        // Schedule a new timer for the event with the remaining time
        val newJob = viewModelScope.launch {
            delay(delayTimeMs) // Wait until it's time for the event to occur
            // call onEventTriggered() with the event type and gameTimeInSeconds
            val eventType = EventType.entries.find { it.ordinal == eventId }
            /* TODO */ //eventType?.let { onEventTriggered(it, gameTimeInSeconds) }

        }

        // Update the eventTimers map with the new job and updated timing information
        eventTimers[eventId] = TimerInfo(newJob, System.currentTimeMillis(), remainingTime)
    }

    // Additional functions as needed
    private fun formatTime(seconds: Int): String {
        val absSeconds = kotlin.math.abs(seconds)
        val formattedTime = String.format(
            "%02d:%02d",
            absSeconds / 60,
            absSeconds % 60
        )
        return if (seconds < 0) "-$formattedTime" else formattedTime
    }

    fun onRoshanKilled() {
        Log.i("MainViewModel", "Roshan killed")
        isRoshanActionEnabled.value = false
        roshanKills.value = roshanKills.value?.plus(1)
        roshanState.value = RoshanState.KILLED
        val gameTimeInSeconds = gameTimeInSeconds
        onEventTriggered(EventType.ROSHAN_KILLED, gameTimeInSeconds, List(1) { "roshan_optimized" })

        // Cancel the existing max respawn timer if it is running
        eventTimers[EventType.ROSHAN_RESPAWN_MAX.ordinal]?.job?.cancel()

        // Start timer for events ROSHAN_RESPAWN_MIN and ROSHAN_RESPAWN_MAX
        val respawnTime = 480 // 8 minutes in seconds
        val minRespawnTime = gameTimeInSeconds + respawnTime
        val maxRespawnTime = gameTimeInSeconds + respawnTime + 180 // 3 minutes in seconds

        minRespawnJob = viewModelScope.launch {
            delay(respawnTime * delay)
            onEventTriggered(EventType.ROSHAN_RESPAWN_MIN, minRespawnTime, List(1) { "roshan_optimized" })
            isRoshanActionEnabled.value = true
            roshanState.value = RoshanState.COULD_RESPAWN
        }
        maxRespawnJob = viewModelScope.launch {
            delay((respawnTime + 180) * delay)
            onEventTriggered(EventType.ROSHAN_RESPAWN_MAX, maxRespawnTime, List(1) { "roshan_optimized" })
            roshanState.value = RoshanState.ALIVE
        }
        aegisDespawnJob = viewModelScope.launch {
            delay(300 * delay)
            onEventTriggered(EventType.AEGIS_DISAPPEARS, gameTimeInSeconds + 300, List(1) { "aegis_optimized" })
            roshanState.value = RoshanState.AEGIS_DISAPPEARED
        }

        eventTimers[EventType.ROSHAN_RESPAWN_MIN.ordinal] = TimerInfo(minRespawnJob!!, System.currentTimeMillis(), respawnTime)
        eventTimers[EventType.ROSHAN_RESPAWN_MAX.ordinal] = TimerInfo(maxRespawnJob!!, System.currentTimeMillis(), respawnTime + 180)
        eventTimers[EventType.AEGIS_DISAPPEARS.ordinal] = TimerInfo(aegisDespawnJob!!, System.currentTimeMillis(), 300)

    }

    fun onDireTormentorKilled() {
        Log.i("MainViewModel", "Dire Tormentor killed")
        isDireTormentorActionEnabled.value = false
        val gameTimeInSeconds = gameTimeInSeconds
        onEventTriggered(EventType.DIRE_TORMENTOR_KILLED, gameTimeInSeconds, List(1) {"dire_tormentor_optimized"})
        val direTormentorRespawnTime = 600 // 10 minutes in seconds
        val direTormentorRespawnJob = viewModelScope.launch {
            delay(direTormentorRespawnTime * delay)
            onEventTriggered(EventType.DIRE_TORMENTOR_RESPAWN, gameTimeInSeconds + direTormentorRespawnTime, List(1) {"dire_tormentor_optimized"})
            isDireTormentorActionEnabled.value = true
        }

        eventTimers[EventType.DIRE_TORMENTOR_KILLED.ordinal] = TimerInfo(direTormentorRespawnJob, System.currentTimeMillis(), direTormentorRespawnTime)
    }

    fun onRadiantTormentorKilled() {
        Log.i("MainViewModel", "Radiant Tormentor killed")
        isRadiantTormentorActionEnabled.value = false
        val gameTimeInSeconds = gameTimeInSeconds
        onEventTriggered(EventType.RADIANT_TORMENTOR_KILLED, gameTimeInSeconds, List(1) { "radiant_tormentor_optimized" })
        val radiantTormentorRespawnTime = 600 // 10 minutes in seconds
        val radiantTormentorRespawnJob = viewModelScope.launch {
            delay(radiantTormentorRespawnTime * delay)
            onEventTriggered(EventType.RADIANT_TORMENTOR_RESPAWN, gameTimeInSeconds + radiantTormentorRespawnTime, List(1) { "radiant_tormentor_optimized" })
            isRadiantTormentorActionEnabled.value = true
        }

        eventTimers[EventType.RADIANT_TORMENTOR_KILLED.ordinal] = TimerInfo(radiantTormentorRespawnJob, System.currentTimeMillis(), radiantTormentorRespawnTime)
    }


    private fun onEventTriggered(eventType: EventType, gameTimeInSeconds: Int, icons: List<String>) {
        Log.i("MainViewModel", "Event triggered: $eventType")
        val eventMessage = when (eventType) {
            EventType.BOUNTY_RUNE -> "Bounty rune"
            EventType.POWER_RUNE -> "Power rune"
            EventType.WISDOM_RUNE -> "Wisdom rune"
            EventType.ROSHAN_RESPAWN_MIN -> "Roshan #${roshanKills.value?.plus(1)} may respawn"
            EventType.ROSHAN_RESPAWN_MAX -> "Roshan #${roshanKills.value?.plus(1)} alive now"
            EventType.TORMENTOR -> "Tormentor available"
            EventType.LOTUS -> "Healing lotus"
            EventType.WATER_RUNE -> "Water rune"
            EventType.ROSHAN_KILLED -> "Roshan killed #${roshanKills.value}"
            EventType.DIRE_TORMENTOR_KILLED -> "Dire tormentor killed"
            EventType.RADIANT_TORMENTOR_KILLED -> "Radiant tormentor killed"
            EventType.NEUTRAL_ITEM_TIER_1 -> "Tier 1 neutral items"
            EventType.NEUTRAL_ITEM_TIER_2 -> "Tier 2 neutral items"
            EventType.NEUTRAL_ITEM_TIER_3 -> "Tier 3 neutral items"
            EventType.NEUTRAL_ITEM_TIER_4 -> "Tier 4 neutral items"
            EventType.NEUTRAL_ITEM_TIER_5 -> "Tier 5 neutral items"
            EventType.GAME_STARTED -> "Game has been started."
            EventType.GAME_PAUSED -> "Game has been paused."
            EventType.GAME_RESUMED -> "Game has been resumed."
            EventType.GAME_ENDED ->  "Game has ended."
            EventType.DIRE_TORMENTOR_RESPAWN -> "Dire tormentor respawn"
            EventType.RADIANT_TORMENTOR_RESPAWN -> "Radiant tormentor respawn"
            EventType.DAY_CYCLE -> "Daytime"
            EventType.NIGHT_CYCLE -> "Nighttime"
            EventType.AEGIS_DISAPPEARS -> "Aegis expires"
        }
        sendNotification(eventType, eventMessage)
        addGameEvent(eventMessage, gameTimeInSeconds, icons)
    }

    private fun addGameEvent(eventMessage: String, occurredGameTimeInSeconds: Int, icons: List<String>) {
        val currentList = occurredGameEvents.value ?: emptyList()
        val eventTime = formatTime(occurredGameTimeInSeconds)
        val updatedList = currentList + FrontendGameEvent(eventMessage, icons, eventTime)
        occurredGameEvents.value = updatedList
    }

    private fun sendNotification(eventType: EventType, eventMessage: String) {
        Log.i("MainViewModel", "Sending notification for $eventType with message: $eventMessage")
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationChannelId = "${eventType.name.lowercase(Locale.getDefault())}_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "${eventType.name} Notification"
            val descriptionText = "Notifies when ${eventType.name.replace('_', ' ').lowercase(Locale.getDefault())} occurs"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(notificationChannelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(getApplication(), notificationChannelId)
            .setSmallIcon(R.drawable.ic_notification) // Make sure you have a generic icon for notifications
            .setContentTitle("${eventType.name.replace('_', ' ')} Spawned")
            .setContentText(eventMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(eventType.ordinal, builder.build()) // Use eventType.ordinal as unique ID for each type of event
    }
}
