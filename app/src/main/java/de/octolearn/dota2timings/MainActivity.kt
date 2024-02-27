package de.octolearn.dota2timings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.octolearn.dota2timings.ui.theme.Dota2TimingsTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    // Assuming occurredGameEvents now includes time information. If it's just messages, you'll need to adjust your data structure.
    val occurredGameEvents by viewModel.occurredGameEvents.observeAsState(emptyList())

    Box(modifier = Modifier.padding(paddingValues)) {
        Column {
            Button(
                onClick = {
                    viewModel.startGame()

                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (gameState) {
                        MainViewModel.GameState.RUNNING -> "⏸ Pause Game"
                        MainViewModel.GameState.PAUSED -> "▶ Resume Game"
                        else -> "▶ Start Game"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            LazyColumn {
                // Reverse the list to show the newest event at the top
                items(occurredGameEvents.reversed()) { eventMessage ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = eventMessage.message, // Assuming eventMessage includes the event description
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        // Assuming you have a way to format or retrieve the event time
                        // This requires storing or associating each event with its timestamp
                        Text(
                            text = eventMessage.timestamp, // Replace with actual event time
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.End,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
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