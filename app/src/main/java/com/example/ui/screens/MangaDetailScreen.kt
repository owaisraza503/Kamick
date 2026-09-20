package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ChapterEntity
import com.example.data.model.DisplayMode
import com.example.data.model.MangaEntity
import com.example.data.model.ReadingDirection
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    mangaId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onReadChapter: (Long, Long, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manga by viewModel.repository.getManga(mangaId).collectAsStateWithLifecycle(initialValue = null)
    val chapters by viewModel.repository.getChapters(mangaId).collectAsStateWithLifecycle(initialValue = emptyList())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showTrackerDialog by remember { mutableStateOf(false) }

    val currentManga = manga
    if (currentManga == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val coverModel = remember(currentManga.coverUri) {
        if (currentManga.coverUri.startsWith("res:")) {
            currentManga.coverUri.removePrefix("res:").toIntOrNull() ?: currentManga.coverUri
        } else {
            currentManga.coverUri
        }
    }

    // Find first unread chapter or last read chapter
    val targetChapter = chapters.find { !it.isRead } ?: chapters.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentManga.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(currentManga.id) }) {
                        Icon(
                            imageVector = if (currentManga.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentManga.favorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showTrackerDialog = true }) {
                        Icon(imageVector = Icons.Default.TrackChanges, contentDescription = "Trackers")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            targetChapter?.let { ch ->
                ExtendedFloatingActionButton(
                    onClick = { onReadChapter(currentManga.id, ch.id, ch.lastPageRead) },
                    icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text(if (ch.lastPageRead > 0) "Resume Ch. ${ch.chapterNumber.toInt()}" else "Read Ch. ${ch.chapterNumber.toInt()}") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("resume_reading_fab")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // HERO HEADER SECTION
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    // Blurred backdrop
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Scrim overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                    )

                    // Foreground Cover + Meta
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier
                                .width(110.dp)
                                .height(160.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(coverModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = currentManga.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = currentManga.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Author: ${currentManga.author}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(currentManga.status, fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            when (currentManga.readingDirection) {
                                                "VERTICAL" -> "Webtoon"
                                                "LTR" -> "Comic"
                                                else -> "Manga"
                                            },
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // DESCRIPTION & STATS
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = currentManga.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // TRACKERS STATUS CARD
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SyncAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tracker Sync",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                                TextButton(onClick = { showTrackerDialog = true }) {
                                    Text("Edit Binding")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TrackerBadge("MAL", currentManga.malId != null)
                                TrackerBadge("AniList", currentManga.anilistId != null)
                                TrackerBadge("Kitsu", currentManga.kitsuId != null)
                                TrackerBadge("MangaUpdates", currentManga.mangaUpdatesId != null)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status: ${currentManga.trackerStatus} • Score: ${if (currentManga.trackerScore > 0) "${currentManga.trackerScore}/10 ★" else "Unrated"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { viewModel.syncTrackersForManga(currentManga.id, currentManga.lastReadChapterNumber.toInt()) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Now", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // CHAPTERS HEADER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${chapters.size} Chapters",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${currentManga.unreadCount} unread",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // CHAPTER ITEMS
            items(chapters, key = { it.id }) { chapter ->
                ChapterRowItem(
                    chapter = chapter,
                    onClick = { onReadChapter(currentManga.id, chapter.id, chapter.lastPageRead) },
                    onToggleRead = {
                        // Toggle read progress
                        // Updated via repository
                    }
                )
            }
        }
    }

    // TRACKER BINDING DIALOG
    if (showTrackerDialog) {
        var malId by remember { mutableStateOf(currentManga.malId ?: "") }
        var anilistId by remember { mutableStateOf(currentManga.anilistId ?: "") }
        var kitsuId by remember { mutableStateOf(currentManga.kitsuId ?: "") }
        var muId by remember { mutableStateOf(currentManga.mangaUpdatesId ?: "") }
        var score by remember { mutableStateOf(currentManga.trackerScore.toFloat()) }
        var status by remember { mutableStateOf(currentManga.trackerStatus) }

        AlertDialog(
            onDismissRequest = { showTrackerDialog = false },
            title = { Text("Trackers for ${currentManga.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Link with your tracker databases:", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = malId,
                        onValueChange = { malId = it },
                        label = { Text("MyAnimeList ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = anilistId,
                        onValueChange = { anilistId = it },
                        label = { Text("AniList ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = kitsuId,
                        onValueChange = { kitsuId = it },
                        label = { Text("Kitsu Slug/ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = muId,
                        onValueChange = { muId = it },
                        label = { Text("MangaUpdates Series ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rating Score: ${score.toInt()}/10")
                    }
                    Slider(
                        value = score,
                        onValueChange = { score = it },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTrackerBinding(
                            mangaId = currentManga.id,
                            malId = malId.ifBlank { null },
                            anilistId = anilistId.ifBlank { null },
                            kitsuId = kitsuId.ifBlank { null },
                            mangaUpdatesId = muId.ifBlank { null },
                            score = score.toInt(),
                            status = status
                        )
                        showTrackerDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrackerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TrackerBadge(serviceName: String, isLinked: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isLinked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (!isLinked) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isLinked) MaterialTheme.colorScheme.primary else Color.Gray)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = serviceName,
                style = MaterialTheme.typography.labelSmall,
                color = if (isLinked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChapterRowItem(
    chapter: ChapterEntity,
    onClick: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (chapter.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (chapter.isRead) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (chapter.isRead) "Read" else "Unread",
                    tint = if (chapter.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (chapter.isRead) FontWeight.Normal else FontWeight.SemiBold
                        ),
                        color = if (chapter.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${chapter.pageCount} pages" + if (chapter.lastPageRead > 0 && !chapter.isRead) " • Page ${chapter.lastPageRead + 1}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
