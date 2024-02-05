package de.octolearn.dota2timings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database: AppDatabase = (application as DotaTimingsApp).database


    private val eventDao = database.eventDao()

    // LiveData to hold your events
    val events = MutableLiveData<List<EventType>>(listOf())
    var gameState = MutableLiveData(GameState.NOT_STARTED)


    private var gameTimerJob: Job? = null
    private var pauseTimerJob: Job? = null

    private var pauseStartTime: Long = 0L
    var gameTime = MutableLiveData("-01:30")
    var pauseTime = MutableLiveData("00:00")

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
            // Assuming getLastPauseEvent() returns the most recent pause event
            val lastPauseEvent = eventDao.getLastPauseEvent() ?: return@launch
            val pauseDuration = System.currentTimeMillis() - lastPauseEvent.timestamp

            // Update all events by adding pauseDuration to their timestamps
            val events = eventDao.getAllEvents()
            events.forEach { event ->
                eventDao.updateEvent(event.copy(timestamp = event.timestamp + pauseDuration))
            }

            // Resume the game timer from the paused point
            resumeGameTimer(pauseDuration.toInt())

            gameState.value = GameState.RUNNING
        }
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
}
