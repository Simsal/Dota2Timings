package de.octolearn.dota2timings

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database: AppDatabase = DotaTimingsApp.database

    private val eventDao = database.eventDao()
    private val gameEventDao = database.gameEventDao()

    // LiveData to hold your events
    val events = MutableLiveData<List<EventType>>(listOf())
    var gameState = MutableLiveData(GameState.NOT_STARTED)
    val occuredGameEvents = MutableLiveData<List<String>>()


    // A data class to hold timer job and elapsed time information
    data class TimerInfo(val job: Job, val startTime: Long, val inGameTime: Int)

    // Map to store timer info for each event
    private val eventTimers = mutableMapOf<Int, TimerInfo>()


    private var gameTimerJob: Job? = null
    private var pauseTimerJob: Job? = null

    private var pauseStartTime: Long = 0L
    var gameTime = MutableLiveData("-01:30")
    var pauseTime = MutableLiveData("00:00")
    var gameTimeInSeconds = -90;    // Starting time (-01:30) in seconds
    var pausedAtSecond: Int? = null

    enum class GameState {
        NOT_STARTED, RUNNING, PAUSED, ENDED
    }

    fun startGame() {
        gameTimerJob?.cancel() // Cancel any existing job



        if (gameState.value == GameState.NOT_STARTED) {
            // Start the game timer logic
            gameTimerJob = viewModelScope.launch {
                while (true) {
                    gameTime.value = formatTime(gameTimeInSeconds)
                    delay(1000) // Delay for 1 second
                    gameTimeInSeconds++
                }
            }
            gameState.value = GameState.RUNNING
            Log.i("MainViewModel", "Game started")
            
            // Start ingame dota event timers, differ between EventType
            scheduleEventTimers()
            
            // Insert game start event into the database
            viewModelScope.launch {
                val startEvent = Event(name = "Game Started", timestamp = System.currentTimeMillis())
                eventDao.insertEvent(startEvent)
            }
        } else if (gameState.value == GameState.RUNNING) {
            // Pause the game
            gameState.value = GameState.PAUSED
        } else if (gameState.value == GameState.PAUSED) {
            // Resume the game
            gameState.value = GameState.RUNNING
        }
    }

    private fun scheduleEventTimers() {
        scheduleBountyRune()
        schedulePowerRune()
        scheduleWisdomRune()
        scheduleFirstTormentorSpawn()
        scheduleLotusSpawn()
    }

    // Schedule the first Bounty Rune spawn at 0 minutes and then every 3 minutes
    private fun scheduleBountyRune() {
        Log.i("MainViewModel", "Scheduling Bounty Rune")
        viewModelScope.launch {
            // Calculate the time until the first spawn
            val initialDelay = if (gameTimeInSeconds < 0) -gameTimeInSeconds else 180 - (gameTimeInSeconds % 180)

            // Initial delay to align with the game time 0 or next 3-minute mark
            delay(initialDelay * 1000L) // Convert seconds to milliseconds

            // Spawn the first rune and then repeat every 3 minutes
            while (isActive) { // isActive is a property of the coroutine scope
                onEventTriggered(EventType.BOUNTY_RUNE)

                // Wait for the next spawn
                delay(180 * 1000L) // 180 seconds or 3 minutes
            }
        }
    }

    // Schedule the first Power Rune spawn at 6 minutes and then every 2 minutes
    private fun schedulePowerRune() {
        viewModelScope.launch {
            // Calculate the time until the first Power Rune spawn
            // If gameTimeInSeconds is less than 360, calculate the delay until it reaches 360.
            // Otherwise, calculate the delay until the next 2-minute mark from the current game time.
            val firstSpawnTime = 360 // 6 minutes in seconds
            val spawnInterval = 120 // 2 minutes in seconds

            val initialDelay = if (gameTimeInSeconds < firstSpawnTime) {
                firstSpawnTime - gameTimeInSeconds
            } else {
                spawnInterval - ((gameTimeInSeconds - firstSpawnTime) % spawnInterval)
            }

            // Initial delay to align with the first Power Rune spawn time or the next 2-minute mark
            delay(initialDelay * 1000L) // Convert seconds to milliseconds

            // Spawn the first rune and then repeat every 2 minutes
            while (isActive) { // isActive is a property of the coroutine scope
                onEventTriggered(EventType.POWER_RUNE)

                // Wait for the next spawn
                delay(spawnInterval * 1000L) // 120 seconds or 2 minutes

            }
        }
    }

    // Schedule the first Wisdom Rune spawn at 7 minutes and then every 7 minutes
    private fun scheduleWisdomRune() {
        viewModelScope.launch {
            // Calculate the time until the first Power Rune spawn
            // If gameTimeInSeconds is less than 360, calculate the delay until it reaches 360.
            // Otherwise, calculate the delay until the next 2-minute mark from the current game time.
            val firstSpawnTime = 420 // 7 minutes in seconds
            val spawnInterval = 420 // 7 minutes in seconds

            val initialDelay = if (gameTimeInSeconds < firstSpawnTime) {
                firstSpawnTime - gameTimeInSeconds
            } else {
                spawnInterval - ((gameTimeInSeconds - firstSpawnTime) % spawnInterval)
            }

            // Initial delay to align with the first Wisdom Rune spawn time or the next 7-minute mark
            delay(initialDelay * 1000L) // Convert seconds to milliseconds

            // Spawn the first rune and then repeat every 7 minutes
            while (isActive) { // isActive is a property of the coroutine scope
                onEventTriggered(EventType.WISDOM_RUNE)

                // Wait for the next spawn
                delay(spawnInterval * 1000L) // 7 minutes
            }
        }
    }
    // Schedule the first Lotus spawn at 3 minutes and then every 3 minutes
    private fun scheduleLotusSpawn() {
        viewModelScope.launch {
            // Calculate the time until the first Lotus spawn
            val firstSpawnTime = 180 // 3 minutes in seconds
            val spawnInterval = 180 // 3 minutes in seconds

            val initialDelay = if (gameTimeInSeconds < firstSpawnTime) {
                firstSpawnTime - gameTimeInSeconds
            } else {
                spawnInterval - ((gameTimeInSeconds - firstSpawnTime) % spawnInterval)
            }

            // Initial delay to align with the first Lotus spawn time or the next 3-minute mark
            delay(initialDelay * 1000L) // Convert seconds to milliseconds

            // Spawn the first Lotus and then repeat every 3 minutes
            while (isActive) { // isActive is a property of the coroutine scope
                onEventTriggered(EventType.LOTUS)

                // Wait for the next spawn
                delay(spawnInterval * 1000L) // 180 seconds or 3 minutes
            }
        }
    }

    // Schedule the first Tormentor spawn at 20 minutes
    private fun scheduleFirstTormentorSpawn() {
        viewModelScope.launch {
            // The spawn time for the first Tormentor
            val spawnTime = 20 * 60 // 20 minutes converted to seconds

            // Calculate the delay until the Tormentor spawn
            // If gameTimeInSeconds is less than spawnTime, calculate the delay until it reaches spawnTime.
            val initialDelay = if (gameTimeInSeconds < spawnTime) {
                spawnTime - gameTimeInSeconds
            } else {
                0 // If for some reason the game time is already past 20 minutes, spawn immediately
            }

            // Wait until the spawn time
            delay(initialDelay * 1000L) // Convert seconds to milliseconds

            onEventTriggered(EventType.TORMENTOR)

        }
    }


    fun pauseGame() {
        gameTimerJob?.cancel() // Stop the game timer
        pausedAtSecond = gameTimeInSeconds // Store the game time at pause

        viewModelScope.launch {
            val pauseEvent = Event(name = "Game Paused", timestamp = pausedAtSecond!! * 1000L)
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

    /*fun resumeGame() {
        viewModelScope.launch {
            val eventsToResume = gameEventDao.getAllEvents() // Or use a specific query to fetch paused events
            eventsToResume.forEach { event ->
                event.remainingTime?.let { remainingTime ->
                    if (remainingTime > 0) {
                        scheduleEventTimer(event, remainingTime)
                    }
                }
            }
        }
        gameState.value = GameState.RUNNING

        // Resume the game timer from the paused point
        resumeGameTimer(pauseDuration.toInt())


    }*/

    private fun resumeGameTimer(pausedTimeInSeconds: Int) {
        gameTimerJob = viewModelScope.launch {
            var timeInSeconds = pausedTimeInSeconds
            while (true) {
                gameTime.value = formatTime(timeInSeconds)
                delay(1000) // Delay for 1 second
                timeInSeconds++
            }
        }
    }

    private fun formatPauseTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }



    fun addEvent(name: String) {
        viewModelScope.launch {
            val event = Event(name = name, timestamp = System.currentTimeMillis())
            eventDao.insertEvent(event)
        }
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


    private fun onEventTriggered(eventType: EventType) {
        Log.i("MainViewModel", "Event triggered: $eventType")
        val eventMessage = when (eventType) {
            EventType.BOUNTY_RUNE -> "A bounty rune has just spawned."
            EventType.POWER_RUNE -> "A power rune is available."
            EventType.WISDOM_RUNE -> TODO()
            EventType.ROSHAN_RESPAWN_MIN -> TODO()
            EventType.ROSHAN_RESPAWN_MAX -> TODO()
            EventType.TORMENTOR -> TODO()
            EventType.LOTUS -> TODO()
        }
        sendNotification(eventType, eventMessage)
    }

    private fun sendNotification(eventType: EventType, eventMessage: String) {
        Log.i("MainViewModel", "Sending notification for $eventType with message: $eventMessage")
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationChannelId = "${eventType.name.toLowerCase()}_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "${eventType.name} Notification"
            val descriptionText = "Notifies when ${eventType.name.replace('_', ' ').toLowerCase()} occurs"
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
