package de.octolearn.dota2timings

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database: AppDatabase = (application as DotaTimingsApp).database

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
    var gameTimeInSeconds = 0;

    enum class GameState {
        NOT_STARTED, RUNNING, PAUSED, ENDED
    }

    fun startGame() {
        gameTimerJob?.cancel() // Cancel any existing job
        var timeInSeconds = -90 // Starting time (-01:30) in seconds

        gameTimerJob = viewModelScope.launch {
            while (true) {
                gameTime.value = formatTime(timeInSeconds)
                delay(1000) // Delay for 1 second
                timeInSeconds++
                gameTimeInSeconds++
            }
        }

        if (gameState.value == GameState.NOT_STARTED) {
            // Start the game timer logic
            gameState.value = GameState.RUNNING
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

    fun pauseGame() {
        gameTimerJob?.cancel() // Stop the game timer
        val currentGameTimeInSeconds =  parseTimeToSeconds(gameTime.value ?: "-01:30")

        viewModelScope.launch {
            val pauseEvent = Event(name = "Game Paused", timestamp = currentGameTimeInSeconds.toLong())
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

    fun resumeGame() {
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


    }

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

    fun scheduleEventTimer(event: GameEvent) {
        val startTime = System.currentTimeMillis()
        val job = viewModelScope.launch {
            // Calculate delay based on event.inGameTime
            delay(event.inGameTime * 1000L)
            // Handle the event occurrence
        }
        eventTimers[event.id] = TimerInfo(job, startTime, event.inGameTime * 1000)
    }

    private fun onEventTriggered(eventType: EventType) {
        val eventMessage = when (eventType) {
            EventType.BOUNTY_RUNE -> "A bounty rune has just spawned."
            EventType.POWER_RUNE -> "A power rune is available."
            EventType.WISDOM_RUNE -> TODO()
            EventType.ROSHAN_RESPAWN_MIN -> TODO()
            EventType.ROSHAN_RESPAWN_MAX -> TODO()
            EventType.TORMENTOR -> TODO()
        }
        sendNotification(eventType, eventMessage)
    }

    private fun sendNotification(eventType: EventType, eventMessage: String) {
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
