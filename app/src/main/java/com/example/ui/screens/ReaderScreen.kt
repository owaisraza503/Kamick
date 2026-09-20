package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.*
import com.example.ui.components.ReaderControlsOverlay
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray

@Composable
fun ReaderScreen(
    mangaId: Long,
    chapterId: Long,
    startPage: Int = 0,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToChapter: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val manga by viewModel.repository.getManga(mangaId).collectAsStateWithLifecycle(initialValue = null)
    val chapters by viewModel.repository.getChapters(mangaId).collectAsStateWithLifecycle(initialValue = emptyList())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentChapter by remember { mutableStateOf<ChapterEntity?>(null) }

    LaunchedEffect(chapterId) {
        currentChapter = viewModel.repository.getChapter(chapterId)
    }

    val activeChapter = currentChapter
    if (activeChapter == null || manga == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    // Parse pages from ChapterEntity
    val pages = remember(activeChapter.pageUrisJson) {
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(activeChapter.pageUrisJson)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (list.isEmpty()) {
            list.add("res:${com.example.R.drawable.manga_page_sample_1789918025455}")
        }
        list
    }

    // Reader State
    var readingDirection by remember(manga) {
        mutableStateOf(
            when (manga?.readingDirection) {
                "LTR" -> ReadingDirection.LTR
                "VERTICAL" -> ReadingDirection.VERTICAL
                else -> ReadingDirection.RTL
            }
        )
    }

    var displayMode by remember(manga) {
        mutableStateOf(
            when (manga?.displayMode) {
                "DOUBLE" -> DisplayMode.DOUBLE
                "WEBTOON" -> DisplayMode.WEBTOON
                else -> DisplayMode.SINGLE
            }
        )
    }

    var readerBackground by remember {
        mutableStateOf(
            when (uiState.settings.readerBackground) {
                "DARK_GRAY" -> ReaderBackground.DARK_GRAY
                "WHITE" -> ReaderBackground.WHITE
                "SEPIA" -> ReaderBackground.SEPIA
                else -> ReaderBackground.BLACK
            }
        )
    }

    var overlayVisible by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(0.95f) }

    // Transform / Zoom State
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    // Chapter navigation references
    val currentChapterIndex = chapters.indexOfFirst { it.id == activeChapter.id }
    val hasPreviousChapter = currentChapterIndex > 0
    val hasNextChapter = currentChapterIndex in 0 until (chapters.size - 1)

    // Keep screen on
    DisposableEffect(uiState.settings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (uiState.settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Pager for Horizontal reading modes
    val pagerState = rememberPagerState(
        initialPage = startPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
        pageCount = { pages.size }
    )

    // LazyListState for Webtoon vertical scrolling
    val webtoonListState = rememberLazyListState(
        initialFirstVisibleItemIndex = startPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    )

    // Current page calculation
    val currentPage = if (displayMode == DisplayMode.WEBTOON) {
        webtoonListState.firstVisibleItemIndex.coerceIn(0, pages.size - 1)
    } else {
        pagerState.currentPage.coerceIn(0, pages.size - 1)
    }

    // Save progress on page changes
    LaunchedEffect(currentPage) {
        val isLastPage = currentPage >= pages.size - 1
        viewModel.repository.updateReadProgress(
            mangaId = mangaId,
            chapterId = activeChapter.id,
            page = currentPage,
            isCompleted = isLastPage
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(readerBackground.hexColor))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val screenWidth = size.width.toFloat()
                        val screenHeight = size.height.toFloat()

                        val normX = if (screenWidth > 0) tapOffset.x / screenWidth else 0.5f
                        val normY = if (screenHeight > 0) tapOffset.y / screenHeight else 0.5f

                        if (normX in 0.30f..0.70f && normY in 0.25f..0.75f) {
                            // Center tap toggles HUD controls
                            overlayVisible = !overlayVisible
                        } else {
                            // Page turning tap zones
                            val isForward = if (readingDirection == ReadingDirection.RTL) normX < 0.35f else normX > 0.65f
                            val isBackward = if (readingDirection == ReadingDirection.RTL) normX > 0.65f else normX < 0.35f

                            if (isForward && currentPage < pages.size - 1) {
                                coroutineScope.launch {
                                    if (displayMode == DisplayMode.WEBTOON) {
                                        webtoonListState.animateScrollToItem(currentPage + 1)
                                    } else {
                                        pagerState.animateScrollToPage(currentPage + 1)
                                    }
                                }
                            } else if (isBackward && currentPage > 0) {
                                coroutineScope.launch {
                                    if (displayMode == DisplayMode.WEBTOON) {
                                        webtoonListState.animateScrollToItem(currentPage - 1)
                                    } else {
                                        pagerState.animateScrollToPage(currentPage - 1)
                                    }
                                }
                            }
                        }
                    },
                    onDoubleTap = {
                        // Double tap to toggle zoom
                        scale = if (scale > 1.2f) 1f else 2.2f
                        offset = Offset.Zero
                    }
                )
            }
    ) {
        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(state = transformState)
        ) {
            if (displayMode == DisplayMode.WEBTOON || readingDirection == ReadingDirection.VERTICAL) {
                // Continuous vertical webtoon reading
                LazyColumn(
                    state = webtoonListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(pages) { index, uri ->
                        val imageModel = remember(uri) {
                            if (uri.startsWith("res:")) {
                                uri.removePrefix("res:").toIntOrNull() ?: uri
                            } else {
                                uri
                            }
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Page ${index + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )
                    }
                }
            } else {
                // Paged mode (Manga RTL or Comic LTR)
                HorizontalPager(
                    state = pagerState,
                    reverseLayout = (readingDirection == ReadingDirection.RTL),
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val pageUri = pages.getOrElse(pageIndex) { "" }
                    val imageModel = remember(pageUri) {
                        if (pageUri.startsWith("res:")) {
                            pageUri.removePrefix("res:").toIntOrNull() ?: pageUri
                        } else {
                            pageUri
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(false)
                                .build(),
                            contentDescription = "Page ${pageIndex + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Screen brightness dimming filter
        if (brightness < 1.0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 1.0f - brightness))
            )
        }

        // Page Indicator HUD (Subtle bottom pill when overlay is hidden)
        if (!overlayVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${currentPage + 1} / ${pages.size}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // FLOATING CONTROLS HUD OVERLAY
        ReaderControlsOverlay(
            visible = overlayVisible,
            chapterTitle = activeChapter.title,
            currentPage = currentPage,
            totalPages = pages.size,
            readingDirection = readingDirection,
            displayMode = displayMode,
            readerBackground = readerBackground,
            brightness = brightness,
            hasPreviousChapter = hasPreviousChapter,
            hasNextChapter = hasNextChapter,
            onBack = onBack,
            onPageChanged = { page ->
                coroutineScope.launch {
                    if (displayMode == DisplayMode.WEBTOON) {
                        webtoonListState.scrollToItem(page)
                    } else {
                        pagerState.scrollToPage(page)
                    }
                }
            },
            onPreviousChapter = {
                if (hasPreviousChapter) {
                    onNavigateToChapter(chapters[currentChapterIndex - 1].id)
                }
            },
            onNextChapter = {
                if (hasNextChapter) {
                    onNavigateToChapter(chapters[currentChapterIndex + 1].id)
                }
            },
            onReadingDirectionChanged = { readingDirection = it },
            onDisplayModeChanged = { displayMode = it },
            onBackgroundChanged = { readerBackground = it },
            onBrightnessChanged = { brightness = it }
        )
    }
}
