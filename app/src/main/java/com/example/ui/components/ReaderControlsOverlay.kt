package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DisplayMode
import com.example.data.model.ReaderBackground
import com.example.data.model.ReadingDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderControlsOverlay(
    visible: Boolean,
    chapterTitle: String,
    currentPage: Int,
    totalPages: Int,
    readingDirection: ReadingDirection,
    displayMode: DisplayMode,
    readerBackground: ReaderBackground,
    brightness: Float,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onBack: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onReadingDirectionChanged: (ReadingDirection) -> Unit,
    onDisplayModeChanged: (DisplayMode) -> Unit,
    onBackgroundChanged: (ReaderBackground) -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsSheet by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to manga info",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = chapterTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "Page ${currentPage + 1} of $totalPages • ${readingDirection.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Reader Settings",
                            tint = Color.White
                        )
                    }
                }
            }

            // BOTTOM BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page scrubber
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousChapter,
                            enabled = hasPreviousChapter,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Chapter",
                                tint = if (hasPreviousChapter) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }

                        Text(
                            text = "${currentPage + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.width(32.dp)
                        )

                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { onPageChanged(it.toInt()) },
                            valueRange = 0f..(totalPages - 1).coerceAtLeast(0).toFloat(),
                            steps = (totalPages - 2).coerceAtLeast(0),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Text(
                            text = "$totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.width(32.dp)
                        )

                        IconButton(
                            onClick = onNextChapter,
                            enabled = hasNextChapter,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Chapter",
                                tint = if (hasNextChapter) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // Mode toggles row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {
                                val nextMode = when (displayMode) {
                                    DisplayMode.SINGLE -> DisplayMode.DOUBLE
                                    DisplayMode.DOUBLE -> DisplayMode.WEBTOON
                                    DisplayMode.WEBTOON -> DisplayMode.SINGLE
                                }
                                onDisplayModeChanged(nextMode)
                            },
                            label = { Text(displayMode.displayName, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (displayMode) {
                                        DisplayMode.SINGLE -> Icons.Default.MenuBook
                                        DisplayMode.DOUBLE -> Icons.Default.AutoStories
                                        DisplayMode.WEBTOON -> Icons.Default.ViewStream
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                labelColor = Color.White,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        AssistChip(
                            onClick = {
                                val nextDir = when (readingDirection) {
                                    ReadingDirection.RTL -> ReadingDirection.LTR
                                    ReadingDirection.LTR -> ReadingDirection.VERTICAL
                                    ReadingDirection.VERTICAL -> ReadingDirection.RTL
                                }
                                onReadingDirectionChanged(nextDir)
                            },
                            label = { Text(readingDirection.displayName.substringBefore(" ("), fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (readingDirection) {
                                        ReadingDirection.RTL -> Icons.AutoMirrored.Filled.ArrowBack
                                        ReadingDirection.LTR -> Icons.AutoMirrored.Filled.ArrowForward
                                        ReadingDirection.VERTICAL -> Icons.Default.ArrowDownward
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                labelColor = Color.White,
                                leadingIconContentColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                }
            }
        }
    }

    // READER SETTINGS BOTTOM SHEET
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Reader Experience",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Reading Direction
                Text(
                    text = "Reading Direction",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadingDirection.values().forEach { dir ->
                        FilterChip(
                            selected = readingDirection == dir,
                            onClick = { onReadingDirectionChanged(dir) },
                            label = { Text(dir.displayName.substringBefore(" ("), fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Display Mode
                Text(
                    text = "Display Mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DisplayMode.values().forEach { mode ->
                        FilterChip(
                            selected = displayMode == mode,
                            onClick = { onDisplayModeChanged(mode) },
                            label = { Text(mode.displayName, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Background Color
                Text(
                    text = "Reader Canvas Background",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderBackground.values().forEach { bg ->
                        FilterChip(
                            selected = readerBackground == bg,
                            onClick = { onBackgroundChanged(bg) },
                            label = { Text(bg.displayName, fontSize = 12.sp) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(bg.hexColor))
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Brightness Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reader Brightness",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${(brightness * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Slider(
                    value = brightness,
                    onValueChange = onBrightnessChanged,
                    valueRange = 0.1f..1.0f
                )
            }
        }
    }
}
