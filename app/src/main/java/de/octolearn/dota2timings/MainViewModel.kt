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

    // LiveData to track if the "Roshan Killed" action is enabled
    val isRoshanActionEnabled = MutableLiveData<Boolean>(true)
    val isDireTormentorActionEnabled = MutableLiveData<Boolean>(false)
    val isRadiantTormentorActionEnabled = MutableLiveData<Boolean>(false)

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

    private var pauseStartTime: Long = 0L
    var gameTime = MutableLiveData("-01:30")
    var pauseTime = MutableLiveData("00:00")
    var gameTimeInSeconds = -90   // Starting time (-01:30) in seconds
    var pauseTimeInSeconds = 0
    var pausedAtSecond: Int? = null

    enum class GameState {
        NOT_STARTED, RUNNING, PAUSED, ENDED
    }

    @NoLiveLiterals
    fun startGame() {
        gameTimerJob?.cancel() // Cancel any existing job



        if (gameState.value == GameState.NOT_STARTED) {
            // Insert game in the database
            viewModelScope.launch {
                val game = Game(startTime = System.currentTimeMillis())
                currentGameId = gameDao.insertGame(game).toInt()

                // Insert game start event into the database
                viewModelScope.launch {
                    val startEvent = Event(name = "Game Started", timestamp = System.currentTimeMillis(), gameId = currentGameId)
                    eventDao.insertEvent(startEvent)
                }
            }

            // Start the game timer logic
            startGameTimer()
            onEventTriggered(EventType.GAME_STARTED, gameTimeInSeconds, List(2) { "dire"; "radiant" })
            gameState.value = GameState.RUNNING
            Log.i("MainViewModel", "Game started")

        } else if (gameState.value == GameState.RUNNING) {
            // Pause the game
            gameState.value = GameState.PAUSED
            pauseGame()
            onEventTriggered(EventType.GAME_PAUSED, gameTimeInSeconds, List(2) { "dire"; "radiant" })

            Log.i("MainViewModel", "Game paused")

            adjustGameTime()
        } else if (gameState.value == GameState.PAUSED) {
            // Resume the game
            gameState.value = GameState.RUNNING
            resumeGameTimer()
            onEventTriggered(EventType.GAME_RESUMED, gameTimeInSeconds, List(2) { "dire"; "radiant" })
            Log.i("MainViewModel", "Game resumed")

        }
    }

    fun endGame() {
        gameTimerJob?.cancel() // Stop the game timer
        pauseTimerJob?.cancel() // Stop the pause timer if running
        gameState.value = GameState.ENDED

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
        // Example for Bounty Rune: spawns every 3 minutes (180 seconds), starting at 0 seconds
        if (gameTimeInSeconds % 180 == 0) {
            onEventTriggered(EventType.BOUNTY_RUNE, gameTimeInSeconds, List(1) { "bounty" })
        }
        // Power Rune: spawns every 2 minutes (120 seconds), starting at 6 minutes (360 seconds)
        if (gameTimeInSeconds >= 240 && (gameTimeInSeconds - 360) % 120 == 0) {

            // add all these runes "haste", "illusion", "invisibility", "regeneration", "amplify_damage", "arcane", "shield"
            onEventTriggered(EventType.POWER_RUNE, gameTimeInSeconds, List(7) { "haste"; "illusion"; "invisibility"; "regeneration"; "amplify_damage"; "arcane"; "shield"})
        }

        // Wisdom Rune: spawns every 7 minutes (420 seconds), starting at 7 minutes (420 seconds)
        if (gameTimeInSeconds >= 420 && (gameTimeInSeconds - 420) % 420 == 0) {
            onEventTriggered(EventType.WISDOM_RUNE, gameTimeInSeconds, List(1) { "wisdom" })
        }

        // Lotus: spawns every 3 minutes (180 seconds), starting at 3 minutes (180 seconds)
        if (gameTimeInSeconds >= 180 && (gameTimeInSeconds - 180) % 180 == 0) {
            onEventTriggered(EventType.LOTUS, gameTimeInSeconds, List(1) { "lotus" })
        }

        // Tormentor: spawns at 20 minutes (1200 seconds)
        if (gameTimeInSeconds == 1200) {
            onEventTriggered(EventType.TORMENTOR, gameTimeInSeconds, List(2) { "dire_tormentor"; "radiant_tormentor" })
            isDireTormentorActionEnabled.value = true
            isRadiantTormentorActionEnabled.value = true
        }

        // Water Rune: spawns at 2 minutes (120 seconds) and 4 minutes (240 seconds)
        if (gameTimeInSeconds == 120 || gameTimeInSeconds == 240) {
            onEventTriggered(EventType.WATER_RUNE, gameTimeInSeconds, List(1) { "water" })
        }

        // Neutral items tier 1: spawn at 7 minutes
        if (gameTimeInSeconds == 420) {
            onEventTriggered(EventType.NEUTRAL_ITEM_TIER_1, gameTimeInSeconds, /*TODO: add icons*/ List(1) {"logo"})
        }

        // Neutral items tier 2: spawn at 17 minutes
        if (gameTimeInSeconds == 1020) {
            onEventTriggered(EventType.NEUTRAL_ITEM_TIER_2, gameTimeInSeconds, /*TODO: add icons*/ List(1) {"logo"})
        }

        // Neutral items tier 3: spawn at 27 minutes
        if (gameTimeInSeconds == 1620) {
            onEventTriggered(EventType.NEUTRAL_ITEM_TIER_3, gameTimeInSeconds, /*TODO: add icons*/ List(1) {"logo"})
        }

        // Neutral items tier 4: spawn at 37 minutes
        if (gameTimeInSeconds == 2220) {
            onEventTriggered(EventType.NEUTRAL_ITEM_TIER_4, gameTimeInSeconds, /*TODO: add icons*/ List(1) {"logo"})
        }

        // Neutral items tier 5: spawn at 60 minutes
        if (gameTimeInSeconds == 3600) {
            onEventTriggered(EventType.NEUTRAL_ITEM_TIER_5, gameTimeInSeconds, /*TODO: add icons*/ List(1) {"logo"})
        }


    }

    private fun startGameTimer() {
        gameTimerJob = viewModelScope.launch {
            while (true) {
                gameTime.value = formatTime(gameTimeInSeconds)
                // Trigger events based on gameTimeInSeconds
                triggerTimedEvents(gameTimeInSeconds)
                //delay(1000) // Delay for 1 second
                delay(100)
                gameTimeInSeconds++
            }
        }
    }


    private fun adjustGameTime() {
        Log.i("MainViewModel", "Adjusting game time")
        viewModelScope.launch {
            // Assuming getLastStartEvent() returns an Event object or null if not found
            val gameStartedEvent = eventDao.getLastStartEvent()

            gameStartedEvent?.let { event ->
                // Assuming the timestamp is stored in milliseconds
                val gameStartedTime = event.timestamp
                val currentTime = System.currentTimeMillis()
                val timePassedSinceGameStarted = (currentTime - gameStartedTime) / 1000

                // Calculate how many seconds have to be incremented to reach the current time
                val incrementsNeeded = timePassedSinceGameStarted - gameTimeInSeconds

                // log the increments needed
                Log.i("MainViewModel", "Increments needed: $incrementsNeeded")

                // Increment gameTimeInSeconds gradually to ensure all events are triggered
                repeat(incrementsNeeded.toInt()) {
                    gameTimeInSeconds++
                    gameTime.postValue(formatTime(gameTimeInSeconds))

                    // Trigger any timed events that should occur at this second
                    triggerTimedEvents(gameTimeInSeconds)
                    delay(1) // Minimal delay to prevent freezing, adjust as needed
                }
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
                //delay(1000) // Delay for 1 second
                delay(100)
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


        startGameTimer()
    }

    private fun rescheduleEvent(eventId: Int, remainingTime: Int) {
        // Cancel the existing timer for the event
        eventTimers[eventId]?.job?.cancel()

        // Assuming remainingTime is in seconds, convert to milliseconds for delay
        val delayTimeMs = remainingTime * 1000L

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

    private fun formatPauseTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
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

    fun parseTimeToSeconds(timeStr: String): Int {
        val negative = timeStr.startsWith("-")
        val parts = timeStr.dropWhile { !it.isDigit() }.split(":").map { it.toInt() }

        // Assuming parts[0] is minutes and parts[1] is seconds
        var totalSeconds = parts[0] * 60 + parts[1]
        if (negative) totalSeconds *= -1

        return totalSeconds
    }

    fun onRoshanKilled() {
        Log.i("MainViewModel", "Roshan killed")
        isRoshanActionEnabled.value = false
        val gameTimeInSeconds = gameTimeInSeconds
        onEventTriggered(EventType.ROSHAN_KILLED, gameTimeInSeconds, List(1) { "roshan" })
        // Start timer for events ROSHAN_RESPAWN_MIN and ROSHAN_RESPAWN_MAX
        val respawnTime = 480 // 8 minutes in seconds
        val minRespawnTime = gameTimeInSeconds + respawnTime
        val maxRespawnTime = gameTimeInSeconds + respawnTime + 180 // 3 minutes in seconds

        val minRespawnJob = viewModelScope.launch {
            // TODO: change to 1000L
            delay(respawnTime * 100L)
            onEventTriggered(EventType.ROSHAN_RESPAWN_MIN, minRespawnTime, List(1) { "roshan" })
            isRoshanActionEnabled.value = true
        }
        val maxRespawnJob = viewModelScope.launch {
            // TODO: change to 1000L
            delay((respawnTime + 180) * 100L)
            onEventTriggered(EventType.ROSHAN_RESPAWN_MAX, maxRespawnTime, List(1) { "roshan" })
        }

        eventTimers[EventType.ROSHAN_RESPAWN_MIN.ordinal] = TimerInfo(minRespawnJob, System.currentTimeMillis(), respawnTime)
        eventTimers[EventType.ROSHAN_RESPAWN_MAX.ordinal] = TimerInfo(maxRespawnJob, System.currentTimeMillis(), respawnTime + 180)


    }

    fun onDireTormentorKilled() {
        Log.i("MainViewModel", "Dire Tormentor killed")
        isDireTormentorActionEnabled.value = false
        val gameTimeInSeconds = gameTimeInSeconds
        onEventTriggered(EventType.DIRE_TORMENTOR_KILLED, gameTimeInSeconds, List(1) {"dire_tormentor"})
        val direTormentorRespawnTime = 600 // 10 minutes in seconds
        val direTormentorRespawnJob = viewModelScope.launch {
            delay(direTormentorRespawnTime * 100L)
            onEventTriggered(EventType.DIRE_TORMENTOR_RESPAWN, gameTimeInSeconds + direTormentorRespawnTime, List(1) {"dire_tormentor"})
            isDireTormentorActionEnabled.value = true
        }

        eventTimers[EventType.DIRE_TORMENTOR_KILLED.ordinal] = TimerInfo(direTormentorRespawnJob, System.currentTimeMillis(), direTormentorRespawnTime)
    }

    fun onRadiantTormentorKilled() {
        Log.i("MainViewModel", "Radiant Tormentor killed")
        isRadiantTormentorActionEnabled.value = false
        val gameTimeInSeconds = gameTimeInSeconds
        onEventTriggered(EventType.RADIANT_TORMENTOR_KILLED, gameTimeInSeconds, List(1) { "radiant_tormentor" })
        val radiantTormentorRespawnTime = 600 // 10 minutes in seconds
        val radiantTormentorRespawnJob = viewModelScope.launch {
            delay(radiantTormentorRespawnTime * 100L)
            onEventTriggered(EventType.RADIANT_TORMENTOR_RESPAWN, gameTimeInSeconds + radiantTormentorRespawnTime, List(1) { "radiant_tormentor" })
            isRadiantTormentorActionEnabled.value = true
        }

        eventTimers[EventType.RADIANT_TORMENTOR_KILLED.ordinal] = TimerInfo(radiantTormentorRespawnJob, System.currentTimeMillis(), radiantTormentorRespawnTime)
    }


    private fun onEventTriggered(eventType: EventType, gameTimeInSeconds: Int, icons: List<String>) {
        Log.i("MainViewModel", "Event triggered: $eventType")
        val eventMessage = when (eventType) {
            EventType.BOUNTY_RUNE -> "A bounty rune has just spawned."
            EventType.POWER_RUNE -> "A power rune is available."
            EventType.WISDOM_RUNE -> "A wisdom rune is available."
            EventType.ROSHAN_RESPAWN_MIN -> "Roshan may respawn soon."
            EventType.ROSHAN_RESPAWN_MAX -> "Roshan must be alive now."
            EventType.TORMENTOR -> "A tormentor has just spawned."
            EventType.LOTUS -> "A lotus has just spawned."
            EventType.WATER_RUNE -> "A water rune is available."
            EventType.ROSHAN_KILLED -> "Roshan has been killed. Respawns in 8-11."
            EventType.DIRE_TORMENTOR_KILLED -> "Dire tormentor has been killed. Respawn in 10."
            EventType.RADIANT_TORMENTOR_KILLED -> "Radiant tormentor has been killed. Respawn in 10."
            EventType.NEUTRAL_ITEM_TIER_1 -> "Tier 1 neutral item available."
            EventType.NEUTRAL_ITEM_TIER_2 -> "Tier 2 neutral item available."
            EventType.NEUTRAL_ITEM_TIER_3 -> "Tier 3 neutral item available."
            EventType.NEUTRAL_ITEM_TIER_4 -> "Tier 4 neutral item available."
            EventType.NEUTRAL_ITEM_TIER_5 -> "Tier 5 neutral item available."
            EventType.GAME_STARTED -> "Game has started."
            EventType.GAME_PAUSED -> "Game has been paused."
            EventType.GAME_RESUMED -> "Game has been resumed."
            EventType.GAME_ENDED ->  "Game has ended."
            EventType.DIRE_TORMENTOR_RESPAWN -> "Dire tormentor has respawned."
            EventType.RADIANT_TORMENTOR_RESPAWN -> "Radiant tormentor has respawned."
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
