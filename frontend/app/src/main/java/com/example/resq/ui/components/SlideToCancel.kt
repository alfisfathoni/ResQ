package com.example.resq.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Slide to cancel slider component matching the "SOS Aktif" reference design:
 * - Pill capsule container with subtle neutral gray background.
 * - Center "Slide to cancel" text.
 * - Draggable circular white thumb with red double chevron (>>).
 * - Dragging beyond threshold triggers [onCancelled].
 * - Releasing before threshold smoothly springs back to start.
 */
@Composable
fun SlideToCancel(
    modifier    : Modifier = Modifier,
    onCancelled : () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val thumbSizeDp = 46.dp
    val thumbPaddingDp = 4.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val thumbPaddingPx = with(density) { thumbPaddingDp.toPx() }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val offsetX = remember { Animatable(0f) }

    val maxDragRange = (containerWidthPx - thumbSizePx - (thumbPaddingPx * 2)).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Color(0xFFE2E4E8))
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .padding(horizontal = thumbPaddingDp),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Center text label "Slide to cancel"
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = "Slide to cancel",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    fontFamily    = PoppinsFamily,
                    fontWeight    = FontWeight.SemiBold,
                    fontSize      = 14.sp,
                    color         = TextPrimary,
                    letterSpacing = 0.2.sp,
                ),
            )
        }

        // Draggable circular thumb
        Surface(
            shape    = CircleShape,
            color    = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(thumbSizeDp)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(maxDragRange) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                val target = (offsetX.value + dragAmount).coerceIn(0f, maxDragRange)
                                offsetX.snapTo(target)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (maxDragRange > 0f && offsetX.value >= maxDragRange * 0.70f) {
                                    // Slide successfully reached end -> cancel SOS
                                    offsetX.animateTo(maxDragRange, tween(150))
                                    onCancelled()
                                    offsetX.snapTo(0f)
                                } else {
                                    // Return to initial position
                                    offsetX.animateTo(0f, tween(250))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, tween(250))
                            }
                        },
                    )
                },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.KeyboardDoubleArrowRight,
                    contentDescription = "Slide Arrow",
                    tint               = ResQRed,
                    modifier           = Modifier.size(24.dp),
                )
            }
        }
    }
}
