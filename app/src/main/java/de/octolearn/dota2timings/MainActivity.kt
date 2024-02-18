package de.octolearn.dota2timings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.octolearn.dota2timings.ui.theme.Dota2TimingsTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Button
import androidx.compose.runtime.livedata.observeAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Dota2TimingsTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    // Use the viewModel instance here
    Scaffold(
        topBar = { AppTopBar(viewModel) },
        floatingActionButton = { MainScreenFAB(viewModel) },
        content = { innerPadding ->
            MainScreenContent(viewModel, innerPadding)
        }
    )
}

@Composable
fun MainScreenContent(viewModel: MainViewModel, paddingValues: PaddingValues) {
    val gameState by viewModel.gameState.observeAsState(MainViewModel.GameState.NOT_STARTED)
    val occuredGameEvents by viewModel.occuredGameEvents.observeAsState(emptyList())

    Box(modifier = Modifier.padding(paddingValues)) {
        Column {
            Button(onClick = {
                if (gameState == MainViewModel.GameState.RUNNING) {
                    viewModel.pauseGame() // Implement pauseGame in your ViewModel
                    viewModel.startPauseTimer()
                } else if (gameState == MainViewModel.GameState.PAUSED) {
                    //viewModel.resumeGame() // Implement resumeGame in your ViewModel
                    viewModel.stopPauseTimer()
                } else {
                    viewModel.startGame()
                }
            }) {
                Text(
                    when (gameState) {
                        MainViewModel.GameState.RUNNING -> "Pause Game"
                        MainViewModel.GameState.PAUSED -> "Resume Game"
                        else -> "Start Game"
                    }
                )
            }
            LazyColumn {
                items(occuredGameEvents) { eventMessage ->
                    Text(text = eventMessage)
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(viewModel: MainViewModel) {
    val gameTime = viewModel.gameTime.observeAsState("-01:30").value

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Game Time: $gameTime", textAlign = TextAlign.Center)
            }
        },
        // Add other parameters like navigationIcon or actions if needed
    )
}


@Composable
fun MainScreenFAB(viewModel: MainViewModel) {
    FloatingActionButton(onClick = {
        // Implement the click action using viewModel here
    }) {
        Icon(Icons.Filled.Add, contentDescription = "Add Event")
    }
}

@Composable
fun EventItem(event: EventType) { // Replace EventType with your event data type
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(event.name, style = MaterialTheme.typography.headlineSmall) // Modified here
            // Add more event details here
        }
    }
}