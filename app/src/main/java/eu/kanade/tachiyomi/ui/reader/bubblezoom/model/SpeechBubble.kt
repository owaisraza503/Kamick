package eu.kanade.tachiyomi.ui.reader.bubblezoom.model

import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.roundToInt

/**
 * Represents a machine-learned detected speech bubble in a comic page.
 *
 * @property id Unique identifier for this speech bubble.
 * @property pageNumber Page index this bubble belongs to.
 * @property rectNormalized Bounding box in normalized coordinates (0.0 to 1.0 relative to page width and height).
 * @property text The recognized dialogue text inside the speech bubble.
 * @property orderIndex Reading order index (0 = first bubble to read on this page).
 */
data class SpeechBubble(
    val id: String,
    val pageNumber: Int,
    val rectNormalized: RectF,
    val text: String,
    val orderIndex: Int,
) {
    /**
     * Checks if a normalized touch coordinate (0.0 to 1.0) lands within this speech bubble,
     * including a small touch slop margin for comfortable tap targeting.
     */
    fun contains(normX: Float, normY: Float, touchSlop: Float = 0.015f): Boolean {
        return normX >= (rectNormalized.left - touchSlop) &&
            normX <= (rectNormalized.right + touchSlop) &&
            normY >= (rectNormalized.top - touchSlop) &&
            normY <= (rectNormalized.bottom + touchSlop)
    }

    /**
     * Converts normalized coordinates to pixel coordinates for image cropping.
     */
    fun toPixelRect(imageWidth: Int, imageHeight: Int): Rect {
        val left = (rectNormalized.left * imageWidth).roundToInt().coerceIn(0, imageWidth)
        val top = (rectNormalized.top * imageHeight).roundToInt().coerceIn(0, imageHeight)
        val right = (rectNormalized.right * imageWidth).roundToInt().coerceIn(left + 1, imageWidth)
        val bottom = (rectNormalized.bottom * imageHeight).roundToInt().coerceIn(top + 1, imageHeight)
        return Rect(left, top, right, bottom)
    }
}
