package eu.kanade.tachiyomi.ui.reader.bubblezoom.detector

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import eu.kanade.tachiyomi.ui.reader.bubblezoom.model.SpeechBubble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Machine learning powered speech bubble detector for comic pages.
 *
 * Uses on-device ML Kit OCR to identify text regions in comic panels,
 * clusters multi-line dialogue balloons, detects speech bubble contour margins,
 * and orders detected speech bubbles according to comic reading direction.
 */
object SpeechBubbleDetector {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Detects all speech bubbles in the provided comic page bitmap.
     *
     * @param bitmap The full or scaled page bitmap.
     * @param pageNumber The 1-based page index.
     * @param isRtl True if reading order is Right-to-Left (e.g. Manga), false for Left-to-Right (Western).
     * @return Ordered list of detected speech bubbles on this page.
     */
    suspend fun detectBubbles(
        bitmap: Bitmap,
        pageNumber: Int,
        isRtl: Boolean = true,
    ): List<SpeechBubble> = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = processImage(inputImage)
            val rawClusters = clusterTextBlocks(visionText, bitmap.width, bitmap.height)

            if (rawClusters.isEmpty()) {
                return@withContext emptyList()
            }

            // Expand text clusters to full speech bubble boundaries
            val bubbles = rawClusters.mapIndexed { index, cluster ->
                val expandedRect = expandToBubbleBounds(cluster.rect, bitmap)
                val normalizedRect = RectF(
                    expandedRect.left.toFloat() / bitmap.width.toFloat(),
                    expandedRect.top.toFloat() / bitmap.height.toFloat(),
                    expandedRect.right.toFloat() / bitmap.width.toFloat(),
                    expandedRect.bottom.toFloat() / bitmap.height.toFloat(),
                )

                RawBubble(
                    id = "${pageNumber}_$index",
                    pageNumber = pageNumber,
                    rect = expandedRect,
                    rectNormalized = normalizedRect,
                    text = cluster.text,
                )
            }

            // Sort bubbles according to comic reading flow (Manga RTL vs Western LTR)
            val sorted = sortInReadingOrder(bubbles, isRtl)

            sorted.mapIndexed { orderIndex, raw ->
                SpeechBubble(
                    id = "${pageNumber}_$orderIndex",
                    pageNumber = pageNumber,
                    rectNormalized = raw.rectNormalized,
                    text = raw.text,
                    orderIndex = orderIndex,
                )
            }
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e) { "Bubble Zoom detection error on page $pageNumber" }
            emptyList()
        }
    }

    private suspend fun processImage(image: InputImage): Text =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    if (cont.isActive) cont.resume(text)
                }
                .addOnFailureListener { error ->
                    if (cont.isActive) cont.resumeWithException(error)
                }
        }

    private data class TextCluster(
        val rect: Rect,
        val text: String,
        val lineCount: Int,
    )

    private data class RawBubble(
        val id: String,
        val pageNumber: Int,
        val rect: Rect,
        val rectNormalized: RectF,
        val text: String,
    )

    /**
     * Groups detected text blocks that belong to the same speech bubble.
     */
    private fun clusterTextBlocks(
        visionText: Text,
        imageWidth: Int,
        imageHeight: Int,
    ): List<TextCluster> {
        val blocks = visionText.textBlocks.filter { block ->
            val box = block.boundingBox ?: return@filter false
            val text = block.text.trim()
            // Ignore isolated 1-character artifacts or boxes outside image
            text.isNotEmpty() && box.width() > 10 && box.height() > 10 &&
                box.width() < (imageWidth * 0.95f) && box.height() < (imageHeight * 0.95f)
        }

        if (blocks.isEmpty()) return emptyList()

        val clusters = mutableListOf<MutableList<Text.TextBlock>>()

        for (block in blocks) {
            val box = block.boundingBox ?: continue
            var merged = false

            for (cluster in clusters) {
                // Check if this block belongs to an existing cluster
                val clusterBox = getBoundingBox(cluster)
                val distanceX = max(0, max(box.left - clusterBox.right, clusterBox.left - box.right))
                val distanceY = max(0, max(box.top - clusterBox.bottom, clusterBox.top - box.bottom))

                val avgHeight = (box.height() + clusterBox.height()) / 2f
                val avgWidth = (box.width() + clusterBox.width()) / 2f

                // In comic balloons, multi-line blocks are vertically close within 1.5x line height
                val isNearby = distanceY < (avgHeight * 0.8f) && distanceX < (avgWidth * 0.5f)
                val overlaps = Rect.intersects(box, clusterBox)

                if (overlaps || isNearby) {
                    cluster.add(block)
                    merged = true
                    break
                }
            }

            if (!merged) {
                clusters.add(mutableListOf(block))
            }
        }

        return clusters.map { cluster ->
            val mergedBox = getBoundingBox(cluster)
            val combinedText = cluster.joinToString("\n") { it.text.trim() }
            val lineCount = cluster.sumOf { it.lines.size }
            TextCluster(mergedBox, combinedText, lineCount)
        }
    }

    private fun getBoundingBox(blocks: List<Text.TextBlock>): Rect {
        var minLeft = Int.MAX_VALUE
        var minTop = Int.MAX_VALUE
        var maxRight = Int.MIN_VALUE
        var maxBottom = Int.MIN_VALUE

        for (block in blocks) {
            val box = block.boundingBox ?: continue
            minLeft = min(minLeft, box.left)
            minTop = min(minTop, box.top)
            maxRight = max(maxRight, box.right)
            maxBottom = max(maxBottom, box.bottom)
        }

        return Rect(minLeft, minTop, maxRight, maxBottom)
    }

    /**
     * Expands the text bounding box to capture the full speech balloon envelope
     * (margins, balloon borders, and tails).
     */
    private fun expandToBubbleBounds(textRect: Rect, bitmap: Bitmap): Rect {
        val width = textRect.width()
        val height = textRect.height()

        // Base geometric expansion: speech bubbles typically extend 20-30% past the text
        val padX = (width * 0.22f).toInt().coerceIn(16, 80)
        val padY = (height * 0.25f).toInt().coerceIn(16, 80)

        var left = (textRect.left - padX).coerceAtLeast(0)
        var top = (textRect.top - padY).coerceAtLeast(0)
        var right = (textRect.right + padX).coerceAtMost(bitmap.width)
        var bottom = (textRect.bottom + padY).coerceAtMost(bitmap.height)

        // Adaptive contour refinement: if the background is light (standard comic balloon),
        // check if there's a balloon outline within a few pixels to snap snugly to it.
        try {
            val centerX = textRect.centerX().coerceIn(0, bitmap.width - 1)
            val centerY = textRect.centerY().coerceIn(0, bitmap.height - 1)
            val centerColor = bitmap.getPixel(centerX, centerY)

            if (isLightColor(centerColor)) {
                // Expand left until contour or limit
                var currLeft = textRect.left
                while (currLeft > max(0, textRect.left - (padX * 1.5f).toInt())) {
                    val pixel = bitmap.getPixel(currLeft, centerY)
                    if (isDarkContour(pixel)) {
                        left = max(0, currLeft - 4)
                        break
                    }
                    currLeft--
                }

                // Expand right until contour or limit
                var currRight = textRect.right
                while (currRight < min(bitmap.width - 1, textRect.right + (padX * 1.5f).toInt())) {
                    val pixel = bitmap.getPixel(currRight, centerY)
                    if (isDarkContour(pixel)) {
                        right = min(bitmap.width, currRight + 4)
                        break
                    }
                    currRight++
                }
            }
        } catch (_: Throwable) {
            // Fallback to geometric expansion if pixel access throws
        }

        return Rect(left, top, right, bottom)
    }

    private fun isLightColor(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
        return luminance > 160.0
    }

    private fun isDarkContour(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
        return luminance < 70.0
    }

    /**
     * Sorts speech bubbles according to comic reading sequence:
     * - Manga (RTL): Top-to-Bottom, Right-to-Left.
     * - Western (LTR): Top-to-Bottom, Left-to-Right.
     */
    private fun sortInReadingOrder(bubbles: List<RawBubble>, isRtl: Boolean): List<RawBubble> {
        if (bubbles.size <= 1) return bubbles

        // Group into approximate horizontal panel bands (within 12% page height)
        val sortedByY = bubbles.sortedBy { it.rectNormalized.top }
        val bands = mutableListOf<MutableList<RawBubble>>()

        for (bubble in sortedByY) {
            val matchingBand = bands.firstOrNull { band ->
                val bandAvgY = band.map { it.rectNormalized.centerY() }.average().toFloat()
                abs(bubble.rectNormalized.centerY() - bandAvgY) < 0.14f
            }

            if (matchingBand != null) {
                matchingBand.add(bubble)
            } else {
                bands.add(mutableListOf(bubble))
            }
        }

        val result = mutableListOf<RawBubble>()
        for (band in bands) {
            // Sort within band: RTL = rightmost first; LTR = leftmost first
            val sortedBand = if (isRtl) {
                band.sortedByDescending { it.rectNormalized.right }
            } else {
                band.sortedBy { it.rectNormalized.left }
            }
            result.addAll(sortedBand)
        }

        return result
    }
}
