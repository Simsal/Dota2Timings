package de.octolearn.dota2timings

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

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


    // A data class to hold timer job and elapsed time information
    data class TimerInfo(val job: Job, val startTime: Long, val inGameTime: Int)

    // A data class to store text + timestamp
    data class FrontendGameEvent(val message: String, val timestamp: String)

    // Map to store timer info for each event
    private val eventTimers = mutableMapOf<Int, TimerInfo>()


    private var gameTimerJob: Job? = null
    private var pauseTimerJob: Job? = null

    private var pauseStartTime: Long = 0L
    var gameTime = MutableLiveData("-01:30")
    var pauseTime = MutableLiveData("00:00")
    var gameTimeInSeconds = -90   // Starting time (-01:30) in seconds
    var pausedAtSecond: Int? = null

    enum class GameState {
        NOT_STARTED, RUNNING, PAUSED, ENDED
    }

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
            gameState.value = GameState.RUNNING
            Log.i("MainViewModel", "Game started")
            

        } else if (gameState.value == GameState.RUNNING) {
            // Pause the game
            gameState.value = GameState.PAUSED
            pauseGame()

            Log.i("MainViewModel", "Game paused")

            adjustGameTime()
        } else if (gameState.value == GameState.PAUSED) {
            // Resume the game
            gameState.value = GameState.RUNNING
            resumeGameTimer()
            Log.i("MainViewModel", "Game resumed")

        }
    }

    private fun triggerTimedEvents(gameTimeInSeconds: Int) {
        // Example for Bounty Rune: spawns every 3 minutes (180 seconds), starting at 0 seconds
        if (gameTimeInSeconds % 180 == 0) {
            onEventTriggered(EventType.BOUNTY_RUNE, gameTimeInSeconds)
        }
        // Power Rune: spawns every 2 minutes (120 seconds), starting at 6 minutes (360 seconds)
        if (gameTimeInSeconds >= 240 && (gameTimeInSeconds - 360) % 120 == 0) {
            onEventTriggered(EventType.POWER_RUNE, gameTimeInSeconds)
        }

        // Wisdom Rune: spawns every 7 minutes (420 seconds), starting at 7 minutes (420 seconds)
        if (gameTimeInSeconds >= 420 && (gameTimeInSeconds - 420) % 420 == 0) {
            onEventTriggered(EventType.WISDOM_RUNE, gameTimeInSeconds)
        }

        // Lotus: spawns every 3 minutes (180 seconds), starting at 3 minutes (180 seconds)
        if (gameTimeInSeconds >= 180 && (gameTimeInSeconds - 180) % 180 == 0) {
            onEventTriggered(EventType.LOTUS, gameTimeInSeconds)
        }

        // Tormentor: spawns at 20 minutes (1200 seconds)
        if (gameTimeInSeconds == 1200) {
            onEventTriggered(EventType.TORMENTOR, gameTimeInSeconds)
        }

        // Water Rune: spawns at 2 minutes (120 seconds) and 4 minutes (240 seconds)
        if (gameTimeInSeconds == 120 || gameTimeInSeconds == 240) {
            onEventTriggered(EventType.WATER_RUNE, gameTimeInSeconds)
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

        viewModelScope.launch {
            val pauseEvent = Event(name = "Game Paused", timestamp = System.currentTimeMillis(), gameId = currentGameId)
            eventDao.insertEvent(pauseEvent)
        }

        startPauseTimer() // Start the pause timer

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
        pauseStartTime = System.currentTimeMillis()

        pauseTimerJob = viewModelScope.launch {
            while (true) {
                val pauseDuration = System.currentTimeMillis() - pauseStartTime
                pauseTime.value = formatPauseTime(pauseDuration)
                delay(1000) // Update every second
            }
        }
    }

    fun stopPauseTimer() {
        pauseTimerJob?.cancel()
        pauseTime.value = "00:00"
        // Optionally save the pause duration to a database
    }

    private fun resumeGameTimer() {
        startGameTimer()
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


    private fun onEventTriggered(eventType: EventType, gameTimeInSeconds: Int) {
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
        }
        sendNotification(eventType, eventMessage)
        addGameEvent(eventMessage, gameTimeInSeconds)
    }

    private fun addGameEvent(eventMessage: String, occurredGameTimeInSeconds: Int) {
        val currentList = occurredGameEvents.value ?: emptyList()
        val eventTime = formatTime(occurredGameTimeInSeconds)
        val updatedList = currentList + FrontendGameEvent(eventMessage, eventTime)
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
