package com.example.resq.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQDarkBadge
import com.example.resq.ui.theme.ResQOuterRing
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The central SOS button matching the design image precisely:
 * - Normal state (Home):
 *   - Soft salmon/pink outer circular ring band with gentle pulse.
 *   - Solid vivid red circle inside with "HELP" text & "HOLD 4S" badge.
 * - Holding state (Count):
 *   - Circular countdown progress ring (salmon base track + sweeping red progress arc).
 *   - White center showing "SOS sent in" and countdown (4s -> 3s -> 2s -> 1s).
 *   - Release before 4s cancels the countdown immediately.
 *   - Full 4s hold triggers [onActivated].
 */
@Composable
fun SosButton(
    modifier        : Modifier = Modifier,
    holdDurationMs  : Long     = 4_000L,
    onActivated     : () -> Unit = {},
    onHoldingChange : (isHolding: Boolean, remainingSeconds: Int, progress: Float) -> Unit = { _, _, _ -> },
) {
    val scope            = rememberCoroutineScope()
    var isHolding        by remember { mutableStateOf(false) }
    var progress         by remember { mutableFloatStateOf(0f) }
    var remainingSeconds by remember { mutableIntStateOf(4) }

    // Breathing pulse animation for the outer ring in normal state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.04f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val pointerModifier = Modifier.pointerInput(holdDurationMs) {
        detectTapGestures(
            onPress = {
                isHolding = true
                progress = 0f
                remainingSeconds = 4
                onHoldingChange(true, 4, 0f)

                val startTime = System.currentTimeMillis()
                val job: Job = scope.launch {
                    val tickMs = 25L
                    while (true) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val curProgress = (elapsed.toFloat() / holdDurationMs).coerceIn(0f, 1f)
                        val remainingMs = (holdDurationMs - elapsed).coerceAtLeast(0L)
                        val curSeconds = ((remainingMs + 999L) / 1000L).toInt().coerceAtLeast(1)

                        progress = curProgress
                        remainingSeconds = curSeconds
                        onHoldingChange(true, curSeconds, curProgress)

                        if (elapsed >= holdDurationMs) {
                            onActivated()
                            break
                        }
                        delay(tickMs)
                    }
                }

                // If user releases finger before 4 seconds, tryAwaitRelease returns
                tryAwaitRelease()
                job.cancel()
                isHolding = false
                progress = 0f
                remainingSeconds = 4
                onHoldingChange(false, 4, 0f)
            },
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier
            .size(246.dp)
            .then(pointerModifier),
    ) {
        if (!isHolding) {
            // ── State 1: Normal HELP Button ("Home") ───────────────────────────

            // Outer thick salmon/pink ring band
            Box(
                modifier = Modifier
                    .size(246.dp)
                    .scale(pulseScale)
                    .background(
                        color = ResQOuterRing.copy(alpha = 0.55f),
                        shape = CircleShape,
                    ),
            )

            // Core Red Button
            Surface(
                shape    = CircleShape,
                color    = ResQRed,
                modifier = Modifier
                    .size(178.dp)
                    .shadow(
                        elevation    = 20.dp,
                        shape        = CircleShape,
                        spotColor    = ResQRed.copy(alpha = 0.45f),
                        ambientColor = ResQRed.copy(alpha = 0.2f),
                    ),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier.background(ResQRed),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(bottom = 4.dp),
                    ) {
                        // "HELP" bold text
                        Text(
                            text  = "HELP",
                            color = Color.White,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily    = PoppinsFamily,
                                fontWeight    = FontWeight.ExtraBold,
                                fontSize      = 38.sp,
                                letterSpacing = 2.sp,
                            ),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // "HOLD 4S" badge with dark red pill background
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .background(
                                    color = ResQDarkBadge,
                                    shape = RoundedCornerShape(50),
                                )
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text  = "HOLD 4S",
                                color = Color.White.copy(alpha = 0.95f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily    = PoppinsFamily,
                                    fontWeight    = FontWeight.Bold,
                                    fontSize      = 10.sp,
                                    letterSpacing = 1.sp,
                                ),
                            )
                        }
                    }
                }
            }
        } else {
            // ── State 2: Countdown Ring ("Count") ─────────────────────────────

            // Circular progress ring on Canvas
            Canvas(modifier = Modifier.size(246.dp)) {
                val strokeWidth = 20.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = this.center

                // Salmon base track ring
                drawCircle(
                    color  = ResQOuterRing.copy(alpha = 0.85f),
                    radius = radius,
                    center = centerOffset,
                    style  = Stroke(width = strokeWidth),
                )

                // Sweeping bright red progress arc
                if (progress > 0f) {
                    val diameter = radius * 2
                    val topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
                    drawArc(
                        color      = ResQRed,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = Size(diameter, diameter),
                        style      = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }

            // White Inner Circle containing "SOS sent in" and "${remainingSeconds}s"
            Surface(
                shape    = CircleShape,
                color    = Color.White,
                modifier = Modifier
                    .size(200.dp)
                    .shadow(
                        elevation    = 6.dp,
                        shape        = CircleShape,
                        spotColor    = ResQRed.copy(alpha = 0.15f),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                    ),
            ) {
                Column(
                    modifier            = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text  = "SOS sent in",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Medium,
                            color      = TextSecondary,
                            fontSize   = 13.sp,
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "${remainingSeconds}s",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color      = ResQRed,
                            fontSize   = 44.sp,
                        ),
                    )
                }
            }
        }
    }
}
