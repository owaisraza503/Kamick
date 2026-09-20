package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrackerEntity
import com.example.ui.viewmodel.LibraryUiState
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackersScreen(
    viewModel: MainViewModel,
    uiState: LibraryUiState,
    modifier: Modifier = Modifier
) {
    var trackerToEdit by remember { mutableStateOf<TrackerEntity?>(null) }
    var inputUsername by remember { mutableStateOf("") }
    var inputToken by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trackers & Sync", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // INFO CARD
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Track Reading Progress",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Connect your favorite tracking services to automatically synchronize read chapters, scores, and status.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // AUTO SYNC SETTING SWITCH
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Sync Reading Progress",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Automatically update your tracker lists whenever you finish reading a chapter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.autoSyncTrackers,
                            onCheckedChange = {
                                viewModel.updateSettings(uiState.settings.copy(autoSyncTrackers = it))
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Supported Tracking Services",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // TRACKER SERVICE CARDS
            items(uiState.trackers) { tracker ->
                TrackerServiceCard(
                    tracker = tracker,
                    onConnect = {
                        trackerToEdit = tracker
                        inputUsername = tracker.username
                        inputToken = tracker.token ?: ""
                    },
                    onDisconnect = {
                        viewModel.disconnectTracker(tracker.service)
                    }
                )
            }
        }
    }

    // CONNECT / EDIT TRACKER DIALOG
    trackerToEdit?.let { tracker ->
        AlertDialog(
            onDismissRequest = { trackerToEdit = null },
            title = { Text("Configure ${tracker.service}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your username and personal access token for ${tracker.service}:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = inputUsername,
                        onValueChange = { inputUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputToken,
                        onValueChange = { inputToken = it },
                        label = { Text("Token / API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputUsername.isNotBlank()) {
                            viewModel.connectTracker(
                                tracker.service,
                                inputUsername.trim(),
                                inputToken.ifBlank { "token_${System.currentTimeMillis()}" }
                            )
                        }
                        trackerToEdit = null
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { trackerToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TrackerServiceCard(
    tracker: TrackerEntity,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (tracker.isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (tracker.service) {
                                "MyAnimeList" -> Icons.Default.Bookmark
                                "AniList" -> Icons.Default.Favorite
                                "Kitsu" -> Icons.Default.Pets
                                else -> Icons.Default.MenuBook
                            },
                            contentDescription = null,
                            tint = if (tracker.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = tracker.service,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (tracker.isConnected) "Connected as ${tracker.username}" else "Not linked",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tracker.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (tracker.isConnected) {
                TextButton(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect")
                }
            } else {
                Button(onClick = onConnect) {
                    Text("Connect")
                }
            }
        }
    }
}
