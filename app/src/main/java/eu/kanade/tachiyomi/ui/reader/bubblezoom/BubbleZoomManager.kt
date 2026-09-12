package eu.kanade.tachiyomi.ui.reader.bubblezoom

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.ui.reader.bubblezoom.crop.BubbleCropper
import eu.kanade.tachiyomi.ui.reader.bubblezoom.detector.SpeechBubbleDetector
import eu.kanade.tachiyomi.ui.reader.bubblezoom.model.SpeechBubble
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Controller and state manager for Google Play Books style machine-learned Bubble Zoom.
 */
class BubbleZoomManager(
    private val preferences: ReaderPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var detectionJob: Job? = null

    // Cache of detected bubbles per page index
    private val pageBubblesCache = ConcurrentHashMap<Int, List<SpeechBubble>>()

    // In-memory stream/bitmap providers for cropping
    private var currentStreamProvider: (() -> InputStream)? = null
    private var currentBitmap: Bitmap? = null
    private var currentPageIndex: Int = -1
    private var isRtlDirection: Boolean = true

    // State flows
    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _magnification = MutableStateFlow(2.0f)
    val magnification: StateFlow<Float> = _magnification.asStateFlow()

    private val _highlightBubbles = MutableStateFlow(false)
    val highlightBubbles: StateFlow<Boolean> = _highlightBubbles.asStateFlow()

    private val _currentBubbles = MutableStateFlow<List<SpeechBubble>>(emptyList())
    val currentBubbles: StateFlow<List<SpeechBubble>> = _currentBubbles.asStateFlow()

    private val _activeBubbleIndex = MutableStateFlow<Int?>(null)
    val activeBubbleIndex: StateFlow<Int?> = _activeBubbleIndex.asStateFlow()

    private val _activeBubbleBitmap = MutableStateFlow<Bitmap?>(null)
    val activeBubbleBitmap: StateFlow<Bitmap?> = _activeBubbleBitmap.asStateFlow()

    private val _isTextReadoutVisible = MutableStateFlow(false)
    val isTextReadoutVisible: StateFlow<Boolean> = _isTextReadoutVisible.asStateFlow()

    private val _isDetecting = MutableStateFlow(false)
    val isDetecting: StateFlow<Boolean> = _isDetecting.asStateFlow()

    init {
        scope.launch {
            preferences.enableBubbleZoom.changes().collectLatest {
                _enabled.value = it
                if (!it) {
                    closeZoom()
                }
            }
        }
        scope.launch {
            preferences.bubbleZoomMagnification.changes().collectLatest { percent ->
                _magnification.value = (percent.toFloat() / 100f).coerceIn(1.2f, 3.5f)
            }
        }
        scope.launch {
            preferences.highlightBubbleZoom.changes().collectLatest {
                _highlightBubbles.value = it
            }
        }
    }

    /**
     * Called when a reader page becomes active.
     */
    fun onPageSelected(page: ReaderPage, isRtl: Boolean) {
        if (!_enabled.value) return

        currentPageIndex = page.index
        isRtlDirection = isRtl
        currentStreamProvider = page.stream

        // Close any active zoom from previous page
        closeZoom()

        // Check if bubbles are already cached for this page
        val cached = pageBubblesCache[page.index]
        if (cached != null) {
            _currentBubbles.value = cached
            return
        }

        // Asynchronously detect speech bubbles using ML Kit
        detectBubblesForPage(page, isRtl)
    }

    /**
     * Sets the loaded page bitmap (e.g. from ReaderPageImageView) to accelerate detection and cropping.
     */
    fun onPageBitmapLoaded(page: ReaderPage, bitmap: Bitmap?, isRtl: Boolean) {
        currentBitmap = bitmap
        currentStreamProvider = page.stream

        if (!_enabled.value) return

        if (!pageBubblesCache.containsKey(page.index)) {
            detectBubblesForPage(page, isRtl, bitmap)
        }
    }

    private fun detectBubblesForPage(page: ReaderPage, isRtl: Boolean, suppliedBitmap: Bitmap? = null) {
        detectionJob?.cancel()
        detectionJob = scope.launch {
            _isDetecting.value = true
            try {
                val bitmapToUse: Bitmap? = suppliedBitmap ?: withContext(Dispatchers.IO) {
                    val streamFn = page.stream ?: return@withContext null
                    try {
                        // Decode at reasonable resolution for fast ML OCR inference
                        streamFn().use { s1 ->
                            val boundsOpt = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeStream(s1, null, boundsOpt)
                            val w = boundsOpt.outWidth
                            val h = boundsOpt.outHeight
                            if (w <= 0 || h <= 0) return@withContext null

                            val sampleSize = maxOf(1, maxOf(w, h) / 1600)
                            streamFn().use { s2 ->
                                val decodeOpt = BitmapFactory.Options().apply {
                                    inSampleSize = sampleSize
                                    inPreferredConfig = Bitmap.Config.RGB_565
                                }
                                BitmapFactory.decodeStream(s2, null, decodeOpt)
                            }
                        }
                    } catch (e: Throwable) {
                        logcat(LogPriority.DEBUG, e) { "Failed decoding bitmap for bubble detection" }
                        null
                    }
                }

                if (bitmapToUse != null) {
                    val bubbles = SpeechBubbleDetector.detectBubbles(bitmapToUse, page.number, isRtl)
                    pageBubblesCache[page.index] = bubbles
                    if (currentPageIndex == page.index) {
                        _currentBubbles.value = bubbles
                    }
                }
            } finally {
                _isDetecting.value = false
            }
        }
    }

    /**
     * Handles tap events from the viewer.
     *
     * @param normX Normalized X coordinate (0.0 to 1.0) on the page image.
     * @param normY Normalized Y coordinate (0.0 to 1.0) on the page image.
     * @return True if the tap was handled by Bubble Zoom, false to let the reader handle it.
     */
    fun onTap(normX: Float, normY: Float): Boolean {
        if (!_enabled.value) return false

        // If a bubble is already zoomed, tap outside closes it
        if (_activeBubbleIndex.value != null) {
            closeZoom()
            return true
        }

        val bubbles = _currentBubbles.value
        if (bubbles.isEmpty()) return false

        // Check if tap hit any detected bubble
        val tappedIndex = bubbles.indexOfFirst { it.contains(normX, normY) }
        if (tappedIndex >= 0) {
            openBubble(tappedIndex)
            return true
        }

        return false
    }

    /**
     * Opens Bubble Zoom for the specified bubble index on the current page.
     */
    fun openBubble(index: Int) {
        val bubbles = _currentBubbles.value
        if (index !in bubbles.indices) return

        val bubble = bubbles[index]
        _activeBubbleIndex.value = index

        scope.launch {
            val crop = BubbleCropper.getCrop(
                bubble = bubble,
                sourceBitmap = currentBitmap,
                streamProvider = currentStreamProvider,
            )
            _activeBubbleBitmap.value = crop
        }
    }

    /**
     * Starts Bubble Zoom from the first bubble or next unread bubble.
     */
    fun startBubbleZoom() {
        val bubbles = _currentBubbles.value
        if (bubbles.isNotEmpty()) {
            openBubble(0)
        }
    }

    /**
     * Cycles to the next speech bubble in reading order.
     */
    fun nextBubble(): Boolean {
        val currentIndex = _activeBubbleIndex.value ?: return false
        val bubbles = _currentBubbles.value
        if (currentIndex < bubbles.lastIndex) {
            openBubble(currentIndex + 1)
            return true
        } else {
            // End of bubbles on this page
            closeZoom()
            return false
        }
    }

    /**
     * Cycles to the previous speech bubble in reading order.
     */
    fun previousBubble(): Boolean {
        val currentIndex = _activeBubbleIndex.value ?: return false
        if (currentIndex > 0) {
            openBubble(currentIndex - 1)
            return true
        }
        return false
    }

    /**
     * Dismisses the active zoomed speech bubble.
     */
    fun closeZoom() {
        _activeBubbleIndex.value = null
        _activeBubbleBitmap.value = null
        _isTextReadoutVisible.value = false
    }

    fun toggleTextReadout() {
        _isTextReadoutVisible.value = !_isTextReadoutVisible.value
    }

    fun clear() {
        detectionJob?.cancel()
        pageBubblesCache.clear()
        BubbleCropper.clearCache()
        closeZoom()
    }
}
