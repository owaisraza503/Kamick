package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.DisplayMode
import com.example.data.model.ReaderBackground
import com.example.data.model.ReadingDirection
import com.example.ui.viewmodel.LibraryUiState
import com.example.ui.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    uiState: LibraryUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var backupStatusMsg by remember { mutableStateOf<String?>(null) }

    // SAF File Picker to restore backup
    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonStr = inputStream?.bufferedReader()?.use { it.readText() }
                if (jsonStr != null) {
                    viewModel.restoreBackup(jsonStr) { success ->
                        backupStatusMsg = if (success) "Library successfully restored!" else "Failed to parse backup JSON."
                    }
                }
            } catch (e: Exception) {
                backupStatusMsg = "Error reading backup file: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
            // APPEARANCE SECTION
            item {
                Text(
                    text = "Appearance & Theme",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Theme Palette",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf("DARK", "LIGHT", "AMOLED", "SYSTEM")
                            themes.forEach { theme ->
                                FilterChip(
                                    selected = uiState.settings.themeMode == theme,
                                    onClick = { viewModel.updateTheme(theme) },
                                    label = { Text(theme, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // READER DEFAULTS SECTION
            item {
                Text(
                    text = "Reader Defaults",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Direction
                        Text("Default Reading Direction", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReadingDirection.values().forEach { dir ->
                                FilterChip(
                                    selected = uiState.settings.defaultReadingDirection == dir.name,
                                    onClick = {
                                        viewModel.updateSettings(uiState.settings.copy(defaultReadingDirection = dir.name))
                                    },
                                    label = { Text(dir.displayName.substringBefore(" ("), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider()

                        // Display Mode
                        Text("Default Display Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DisplayMode.values().forEach { mode ->
                                FilterChip(
                                    selected = uiState.settings.defaultDisplayMode == mode.name,
                                    onClick = {
                                        viewModel.updateSettings(uiState.settings.copy(defaultDisplayMode = mode.name))
                                    },
                                    label = { Text(mode.displayName, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider()

                        // Keep screen on switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Keep Screen On", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("Prevents device sleep while reading", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.settings.keepScreenOn,
                                onCheckedChange = {
                                    viewModel.updateSettings(uiState.settings.copy(keepScreenOn = it))
                                }
                            )
                        }
                    }
                }
            }

            // EXTENSIONS & SOURCES SECTION
            item {
                Text(
                    text = "Extensions & Sources",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Tachiyomi Extension Engine", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                Text("Enable modular manga/comic source extensions with online catalogs, chapter updates, and direct reading.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Auto-Update Extensions", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("Keep installed comic source plugins up to date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = true,
                                onCheckedChange = { /* toggle auto update */ }
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Auto-Check Extension Repos", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("Automatically check for new sources and updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = true,
                                onCheckedChange = { /* toggle check */ }
                            )
                        }
                    }
                }
            }

            // BACKUP & RESTORE SECTION
            item {
                Text(
                    text = "Backup & Cloud Export",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Save your library, categories, read progress, and tracker links for offline use or cloud synchronization (Google Drive, Dropbox, local storage).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        backupStatusMsg?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.exportBackup { jsonString ->
                                        try {
                                            val backupFile = File(context.cacheDir, "kamick_backup_${System.currentTimeMillis()}.json")
                                            FileOutputStream(backupFile).use { it.write(jsonString.toByteArray()) }

                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                backupFile
                                            )

                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Save or Upload Kamick Backup"))
                                            backupStatusMsg = "Backup file created!"
                                        } catch (e: Exception) {
                                            // Fallback share as text
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, jsonString)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Export Kamick Backup JSON"))
                                            backupStatusMsg = "Backup exported!"
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Backup")
                            }

                            OutlinedButton(
                                onClick = {
                                    restorePicker.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore")
                            }
                        }
                    }
                }
            }

            // ABOUT KAMICK
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Kamick Reader",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Version 1.0.0 • Package: com.raza.kamick",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Free & Open Source manga, webtoon, and comic reader with Bubble Zoom, scheduled library updates, and tracker integration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
