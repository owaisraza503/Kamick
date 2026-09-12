package eu.kanade.tachiyomi.ui.reader.bubblezoom.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import android.util.LruCache
import eu.kanade.tachiyomi.ui.reader.bubblezoom.model.SpeechBubble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Handles high-resolution cropping and caching of detected speech bubbles.
 */
object BubbleCropper {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // 12.5% of heap for bubble crop cache

    private val cropCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Retrieves or creates a cropped bitmap of the given speech bubble.
     */
    suspend fun getCrop(
        bubble: SpeechBubble,
        sourceBitmap: Bitmap? = null,
        streamProvider: (() -> InputStream)? = null,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = bubble.id
        cropCache.get(cacheKey)?.let { return@withContext it }

        // Attempt RegionDecoder from stream first for full resolution
        if (streamProvider != null) {
            try {
                streamProvider().use { stream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    // Mark/reset or decode bounds
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    streamProvider().use { BitmapFactory.decodeStream(it, null, boundsOptions) }
                    val imgW = boundsOptions.outWidth
                    val imgH = boundsOptions.outHeight

                    if (imgW > 0 && imgH > 0) {
                        val pixelRect = bubble.toPixelRect(imgW, imgH)
                        // Add 10% breathing margin around bubble
                        val marginX = (pixelRect.width() * 0.1f).roundToInt()
                        val marginY = (pixelRect.height() * 0.1f).roundToInt()
                        val cropRect = Rect(
                            max(0, pixelRect.left - marginX),
                            max(0, pixelRect.top - marginY),
                            min(imgW, pixelRect.right + marginX),
                            min(imgH, pixelRect.bottom + marginY),
                        )

                        val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            BitmapRegionDecoder.newInstance(stream)
                        } else {
                            @Suppress("DEPRECATION")
                            BitmapRegionDecoder.newInstance(stream, false)
                        }

                        val cropped = decoder?.decodeRegion(cropRect, BitmapFactory.Options())
                        decoder?.recycle()

                        if (cropped != null) {
                            cropCache.put(cacheKey, cropped)
                            return@withContext cropped
                        }
                    }
                }
            } catch (e: Throwable) {
                logcat(LogPriority.DEBUG, e) { "BitmapRegionDecoder fallback to sourceBitmap" }
            }
        }

        // Fallback to cropping from loaded page bitmap
        if (sourceBitmap != null && !sourceBitmap.isRecycled) {
            try {
                val pixelRect = bubble.toPixelRect(sourceBitmap.width, sourceBitmap.height)
                val marginX = (pixelRect.width() * 0.1f).roundToInt()
                val marginY = (pixelRect.height() * 0.1f).roundToInt()
                val cropLeft = max(0, pixelRect.left - marginX)
                val cropTop = max(0, pixelRect.top - marginY)
                val cropWidth = min(sourceBitmap.width - cropLeft, pixelRect.width() + marginX * 2)
                val cropHeight = min(sourceBitmap.height - cropTop, pixelRect.height() + marginY * 2)

                if (cropWidth > 0 && cropHeight > 0) {
                    val cropped = Bitmap.createBitmap(sourceBitmap, cropLeft, cropTop, cropWidth, cropHeight)
                    cropCache.put(cacheKey, cropped)
                    return@withContext cropped
                }
            } catch (e: Throwable) {
                logcat(LogPriority.WARN, e) { "Error cropping bubble from bitmap" }
            }
        }

        null
    }

    fun clearCache() {
        cropCache.evictAll()
    }
}
