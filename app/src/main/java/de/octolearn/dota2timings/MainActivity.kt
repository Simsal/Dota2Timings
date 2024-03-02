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
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

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
    val occurredGameEvents by viewModel.occurredGameEvents.observeAsState(emptyList())

    // Reverse to make the latest events appear first, and then split the list.
    val latestFiveEvents = occurredGameEvents.takeLast(5).reversed()
    val olderEvents = occurredGameEvents.dropLast(5).reversed()

    Box(modifier = Modifier.padding(paddingValues)) {
        Column {
            // Check if the game is either running or paused to show the split buttons
            if (gameState == MainViewModel.GameState.RUNNING || gameState == MainViewModel.GameState.PAUSED) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (gameState == MainViewModel.GameState.RUNNING) viewModel.pauseGame()
                            else viewModel.startGame()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RectangleShape // Apply RectangleShape to remove rounded corners

                    ) {
                        Text(
                            text = if (gameState == MainViewModel.GameState.RUNNING) "⏸ Pause Game" else "▶ Resume Game",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Button(
                        onClick = {
                            // Add your end game logic here
                            viewModel.endGame()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RectangleShape // Apply RectangleShape to remove rounded corners

                    ) {
                        Text("End Game", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                // If the game is not started or in any other state, show the start game button
                Button(
                    onClick = viewModel::startGame,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape // Apply RectangleShape to remove rounded corners

                ) {
                    Text(
                        text = "▶ Start Game",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            LazyColumn {
                // Section for the latest five events, displayed prominently
                items(latestFiveEvents) { event ->
                    EventCard(event = event, isProminent = true)
                }
                // Section for older events, standard display
                items(olderEvents) { event ->
                    EventCard(event = event, isProminent = false)
                }
            }
        }
    }
}

@Composable
fun EventCard(event: MainViewModel.FrontendGameEvent, isProminent: Boolean) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = event.message,
                        style = if (isProminent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // Assuming event.iconIds is a list of drawable resource IDs
                    Row {
                        event.iconIds.take(6).forEach { iconName ->
                            val resourceId = context.resources.getIdentifier(
                                iconName,
                                "drawable",
                                context.packageName
                            )
                            if (resourceId != 0) { // If the resource was found
                                Icon(
                                    painter = painterResource(id = resourceId),
                                    contentDescription = "Event Icon",
                                    modifier = Modifier.size(if (isProminent) 36.dp else 24.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = event.timestamp,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(viewModel: MainViewModel) {
    val gameState by viewModel.gameState.observeAsState(MainViewModel.GameState.NOT_STARTED)
    val gameTime by viewModel.gameTime.observeAsState("-01:30")
    val pauseTime by viewModel.pauseTime.observeAsState("00:00")

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                when (gameState) {
                    MainViewModel.GameState.RUNNING -> {

                        Text(
                            text = gameTime,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    MainViewModel.GameState.PAUSED -> {

                        Text(
                            text = pauseTime,
                            color = Color.Yellow,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    else -> {

                        Text(
                            text = gameTime,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        },
        // Adjust the colors, actions, and other properties of TopAppBar as needed
    )
}


@Composable
fun MainScreenFAB(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val isRoshanActionEnabled by viewModel.isRoshanActionEnabled.observeAsState(false)
    val isDireTormentorActionEnabled by viewModel.isDireTormentorActionEnabled.observeAsState(false)
    val isRadiantTormentorActionEnabled by viewModel.isRadiantTormentorActionEnabled.observeAsState(false)

    Box(modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.BottomEnd)) {
        FloatingActionButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Add, contentDescription = "Add Event")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Roshan Killed") },
                onClick = {
                    viewModel.onRoshanKilled() // Handle the "Roshan Killed" event
                    expanded = false // Dismiss the menu after selection
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete, // Just an example, choose an appropriate icon
                        contentDescription = "Roshan Killed"
                    )
                },
                enabled = isRoshanActionEnabled
            )
            DropdownMenuItem(
                text = { Text("Dire Tormentor killed") },
                onClick = {
                    viewModel.onDireTormentorKilled() // Handle the "Roshan Killed" event
                    expanded = false // Dismiss the menu after selection
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete, // Just an example, choose an appropriate icon
                        contentDescription = "Dire Tormentor killed"
                    )
                },
                enabled = isDireTormentorActionEnabled
            )
            DropdownMenuItem(
                text = { Text("Radiant Tormentor killed") },
                onClick = {
                    viewModel.onRadiantTormentorKilled() // Handle the "Roshan Killed" event
                    expanded = false // Dismiss the menu after selection
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete, // Just an example, choose an appropriate icon
                        contentDescription = "Radiant Tormentor killed"
                    )
                },
                enabled = isRadiantTormentorActionEnabled
            )
            // Add more DropdownMenuItem(s) as needed
        }
    }
}




