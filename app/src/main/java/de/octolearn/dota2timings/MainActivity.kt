package de.octolearn.dota2timings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.rememberImagePainter

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

    // Reverse the list to have the most recent events first
    val reversedEvents = occurredGameEvents.reversed()

    // Split the events based on your criteria
    val mostRecentEvents = reversedEvents.take(4) // Most recent 4 events
    val lessProminentEvents = reversedEvents.drop(4).take(3) // Next 3 events
    val standardListEvents = reversedEvents.drop(7) // Rest of the events

    Box(modifier = Modifier.padding(paddingValues)) {
        Column {
            // Check if the game is either running or paused to show the split buttons
            if (gameState == MainViewModel.GameState.RUNNING || gameState == MainViewModel.GameState.PAUSED) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.startGame()
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
                Row(modifier = Modifier.fillMaxWidth()) {
                    // If the game is not started or in any other state, show the start game button
                    Button(
                        onClick = viewModel::startGame,
                        modifier = Modifier.weight(1f),
                        shape = RectangleShape // Apply RectangleShape to remove rounded corners

                    ) {
                        Text(
                            text = "▶ Quickstart Game",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = viewModel::showSetupGameDialog,
                        modifier = Modifier.weight(1f),

                        shape = RectangleShape) {
                        Text("Set Up Game")
                    }
                }
            }

            LazyColumn {
                // Section for the latest five events, displayed prominently
                items(mostRecentEvents) { event ->
                    EventCard(event = event, isProminent = true)
                }
                // Section for older events, standard display
                items(lessProminentEvents) { event ->
                    EventCard(event = event, isProminent = false)
                }
                // Section for even older events, display as list with divider without card or icons text on the left side and time on the right side
                items(standardListEvents) { event ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = event.message, // Adjust as per your data structure
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = event.timestamp, // Adjust as per your data structure
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.End,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        Divider()
                    }
                }


            }
        }
        // Check and display dialogs at the end for better organization
        GameSetupDialogIfNeeded(viewModel = viewModel)
    }
}

@Composable
fun GameSetupDialogIfNeeded(viewModel: MainViewModel) {
    val showDialog by viewModel.showDialog.observeAsState(false)

    if (showDialog) {
        GameSetupDialog(viewModel = viewModel) {
            viewModel.hideSetupGameDialog()
        }
    }
}

@Composable
fun GameSetupDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val heroesWithAbilities by viewModel.heroesWithAbilities.observeAsState(initial = emptyList())
    val expandedHeroId = remember { mutableStateOf<String?>(null) }
    val uiState by viewModel.uiState.observeAsState()

    when (uiState) {
        is MainViewModel.UiState.Loading -> CircularProgressIndicator()
        is MainViewModel.UiState.Error -> Text("Error: ${(uiState as MainViewModel.UiState.Error).message}")
        is MainViewModel.UiState.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Game Setup") },
                text = {
                    LazyColumn {
                        items(heroesWithAbilities) { hero ->
                            HeroSelectionItem(
                                hero = hero,
                                isExpanded = expandedHeroId.value == hero.id,
                                onHeroSelected = { isSelected ->
                                    viewModel.toggleHeroSelection(hero.id, isSelected)
                                },
                                onExpandClicked = {
                                    expandedHeroId.value = if (expandedHeroId.value == hero.id) null else hero.id
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        //viewModel.finalizeGameSetup()
                        onDismiss()
                    }) {
                        Text("Start Game")
                    }
                }
            )
        }

        null -> {
            // No-op
        }
    }


}

@Composable
fun HeroSelectionItem(
    hero: MainViewModel.HeroWithAbilities, // Assume this includes id, name, and abilities (each with name and imageURL)
    isExpanded: Boolean,
    onHeroSelected: (Boolean) -> Unit,
    onExpandClicked: () -> Unit
) {
    var isSelected by remember { mutableStateOf(false) }
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(4.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onExpandClicked() }
                    .fillMaxWidth()) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = {
                        isSelected = it
                        onHeroSelected(it)
                    }
                )
                Text(text = hero.name, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = { onExpandClicked() }) {
                    Icon(imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand")
                }
            }
            if (isExpanded) {
                AbilitiesList(abilities = hero.abilities)
            }
        }
    }
}

@Composable
fun AbilitiesList(abilities: List<MainViewModel.Ability>) {
    LazyColumn {
        items(abilities) { ability ->
            AbilityItem(ability = ability)
        }
    }
}

@Composable
fun AbilityItem(ability: MainViewModel.Ability) {
    // Improved Image loading with placeholders and error handling
    val painter = rememberImagePainter(
        data = ability.imageUrl,
        builder = {
            crossfade(true)
            placeholder(R.drawable.dire_optimized)
            error(R.drawable.radiant_optimized)
        }
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
        Image(
            painter = painter,
            contentDescription = ability.name, // Accessibility improvement
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = ability.name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EventCard(event: MainViewModel.FrontendGameEvent, isProminent: Boolean) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(color = Color.White), // Set the card background to white
        elevation = CardDefaults.cardElevation(if (isProminent) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = event.message,
                style = if (isProminent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.Start)
            )

            // Icons row, centered and with bigger icons for prominent events
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                event.iconIds.take(6).forEach { iconName ->
                    val resourceId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                    if (resourceId != 0) { // If the resource was found
                        Icon(
                            painter = painterResource(id = resourceId),
                            contentDescription = "Event Icon",
                            modifier = Modifier.size(if (isProminent) 60.dp else 36.dp), // Larger icons for prominent events
                            tint = Color.Unspecified
                        )
                    }
                }
            }

            // In-game time on the right side
            Text(
                text = event.timestamp,
                style = if (isProminent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(viewModel: MainViewModel) {
    val roshanKills by viewModel.roshanKills.observeAsState(0)
    val isNight by viewModel.isNight.observeAsState(true)
    val gameState by viewModel.gameState.observeAsState(MainViewModel.GameState.NOT_STARTED)
    val gameTime by viewModel.gameTime.observeAsState("-01:30")
    val pauseTime by viewModel.pauseTime.observeAsState("00:00")
    val roshanState by viewModel.roshanState.observeAsState(MainViewModel.RoshanState.ALIVE)


    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Roshan kills on the left
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Start) {
                    when (roshanState) {
                        MainViewModel.RoshanState.ALIVE -> {
                            Icon(
                                painter = painterResource(id = R.drawable.roshan_optimized),
                                contentDescription = "Roshan Alive",
                                modifier = Modifier.size(30.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "#${roshanKills + 1}",
                                color = Color.Black
                            )
                        }
                        MainViewModel.RoshanState.KILLED -> {
                            Icon(
                                painter = painterResource(id = R.drawable.aegis_optimized),
                                contentDescription = "Aegis",
                                modifier = Modifier.size(30.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "#$roshanKills",
                                color = Color.Black
                            )
                        }
                        MainViewModel.RoshanState.AEGIS_DISAPPEARED -> {
                            Icon(
                                painter = painterResource(id = R.drawable.roshan_clock_optimized),
                                contentDescription = "Roshan Clock",
                                modifier = Modifier.size(42.dp),
                                tint = Color.Unspecified
                            )

                        }

                        MainViewModel.RoshanState.COULD_RESPAWN -> {
                            Icon(
                                painter = painterResource(id = R.drawable.roshan_clock_optimized),
                                contentDescription = "Roshan Clock",
                                modifier = Modifier.size(42.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.roshan_optimized),
                                contentDescription = "Roshan Could Be Up",
                                modifier = Modifier.size(30.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }
                }

                // Centered game time or pause time
                Text(
                    text = when (gameState) {
                        MainViewModel.GameState.RUNNING -> gameTime
                        MainViewModel.GameState.PAUSED -> pauseTime
                        else -> gameTime
                    },
                    color = if (gameState == MainViewModel.GameState.PAUSED) MaterialTheme.colorScheme.primary else Color.Black,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                // Moon or Sun icon on the right
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    if (isNight) {
                    Icon(
                        painter = painterResource(id = R.drawable.moon),
                        contentDescription = if (isNight) "Night" else "Day",
                        modifier = Modifier.size(30.dp)
                    )} else {
                        Icon(
                            painter = painterResource(id = R.drawable.sun),
                            contentDescription = if (isNight) "Night" else "Day",
                            modifier = Modifier.size(30.dp)
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
                        painter = painterResource(id = R.drawable.roshan_optimized),
                        contentDescription = "Roshan",
                        modifier = Modifier.size(30.dp),
                        tint = Color.Unspecified
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
                        painter = painterResource(id = R.drawable.dire_tormentor_optimized),
                        contentDescription = "Dire Tormentor",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
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
                        painter = painterResource(id = R.drawable.radiant_tormentor_optimized),
                        contentDescription = "Radiant Tormentor",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                },
                enabled = isRadiantTormentorActionEnabled
            )
            // Add more DropdownMenuItem(s) as needed
        }
    }
}




