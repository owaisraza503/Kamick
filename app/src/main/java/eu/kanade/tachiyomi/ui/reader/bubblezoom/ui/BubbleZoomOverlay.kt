package eu.kanade.tachiyomi.ui.reader.bubblezoom.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.bubblezoom.BubbleZoomManager
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.EditNote
import kotlin.math.roundToInt

/**
 * Modern Google Play Books-style Bubble Zoom overlay.
 *
 * Displays machine-learned speech bubble magnification, reading-order navigation,
 * optional OCR text readout, and subtle highlight indicators.
 */
@Composable
fun BubbleZoomOverlay(
    manager: BubbleZoomManager,
    modifier: Modifier = Modifier,
) {
    val enabled by manager.enabled.collectAsState()
    if (!enabled) return

    val currentBubbles by manager.currentBubbles.collectAsState()
    val activeIndex by manager.activeBubbleIndex.collectAsState()
    val activeBitmap by manager.activeBubbleBitmap.collectAsState()
    val highlightBubbles by manager.highlightBubbles.collectAsState()
    val isTextReadoutVisible by manager.isTextReadoutVisible.collectAsState()
    val magnification by manager.magnification.collectAsState()

    BackHandler(enabled = activeIndex != null) {
        manager.closeZoom()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Optional speech bubble highlight overlays when enabled
        if (highlightBubbles && activeIndex == null && currentBubbles.isNotEmpty()) {
            currentBubbles.forEachIndexed { index, bubble ->
                val bubbleLeft = (bubble.rectNormalized.left * screenWidth.value).dp
                val bubbleTop = (bubble.rectNormalized.top * screenHeight.value).dp
                val bubbleWidth = (bubble.rectNormalized.width() * screenWidth.value).dp
                val bubbleHeight = (bubble.rectNormalized.height() * screenHeight.value).dp

                Box(
                    modifier = Modifier
                        .offset(bubbleLeft, bubbleTop)
                        .size(bubbleWidth, bubbleHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            manager.openBubble(index)
                        },
                )
            }
        }

        // Active Zoomed Bubble View
        val currentBubble = activeIndex?.let { currentBubbles.getOrNull(it) }

        AnimatedVisibility(
            visible = currentBubble != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
        ) {
            // Scrim to focus on bubble and capture outside taps
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        manager.closeZoom()
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (currentBubble != null) {
                    val total = currentBubbles.size
                    val currentIndex = activeIndex ?: 0

                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .widthIn(min = 280.dp, max = 520.dp)
                            .shadow(24.dp, RoundedCornerShape(22.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                // Prevent dismissing when clicking card interior
                            },
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            // Header bar: Bubble indicator badge & controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Bubble badge
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_bubble_zoom_24dp),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Bubble ${currentIndex + 1} of $total",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                            ),
                                        )
                                    }
                                }

                                // Navigation buttons: Prev, Next, OCR Readout, Close
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Previous bubble
                                    IconButton(
                                        onClick = { manager.previousBubble() },
                                        enabled = currentIndex > 0,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                            contentDescription = "Previous bubble",
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }

                                    // Next bubble
                                    IconButton(
                                        onClick = { manager.nextBubble() },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbols.AutoMirroredRounded.ArrowForward,
                                            contentDescription = "Next bubble",
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }

                                    // Toggle recognized text
                                    if (currentBubble.text.isNotBlank()) {
                                        IconButton(
                                            onClick = { manager.toggleTextReadout() },
                                            modifier = Modifier.size(36.dp),
                                            colors = if (isTextReadoutVisible) {
                                                IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                )
                                            } else {
                                                IconButtonDefaults.iconButtonColors()
                                            },
                                        ) {
                                            Icon(
                                                imageVector = MaterialSymbols.Rounded.EditNote,
                                                contentDescription = "Toggle text transcript",
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }

                                    // Close button
                                    IconButton(
                                        onClick = { manager.closeZoom() },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbols.Rounded.Close,
                                            contentDescription = "Close zoom",
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.size(10.dp))

                            // Magnified high-res bubble crop
                            if (activeBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White)
                                        .clickable {
                                            // Tapping the image advances to the next bubble
                                            manager.nextBubble()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        bitmap = activeBitmap!!.asImageBitmap(),
                                        contentDescription = currentBubble.text,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 140.dp, max = 360.dp),
                                    )
                                }
                            } else {
                                // Loading placeholder while region is cropped
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 140.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Loading bubble...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Optional OCR text transcription
                            AnimatedVisibility(
                                visible = isTextReadoutVisible && currentBubble.text.isNotBlank(),
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .padding(top = 10.dp)
                                        .fillMaxWidth()
                                        .heightIn(max = 140.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .verticalScroll(rememberScrollState()),
                                    ) {
                                        Text(
                                            text = currentBubble.text,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                lineHeight = 20.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            // Tap hint for quick navigation
                            Text(
                                text = "Tap bubble to advance • Tap outside to close",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .align(Alignment.CenterHorizontally),
                            )
                        }
                    }
                }
            }
        }
    }
}
