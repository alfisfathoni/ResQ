package com.example.resq.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQDarkBadge
import com.example.resq.ui.theme.ResQOuterRing
import com.example.resq.ui.theme.ResQRed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The central SOS button matching the design image precisely:
 * - A large soft salmon/pink outer circular ring band.
 * - Solid vivid red circle inside with a soft red drop shadow.
 * - Bold white "HELP" text.
 * - Pill badge with dark red background and "HOLD 4S" text.
 * - Press-and-hold gesture timer (4 seconds) with tactile feedback.
 */
@Composable
fun SosButton(
    modifier       : Modifier = Modifier,
    holdDurationMs : Long     = 4_000L,
    onActivated    : () -> Unit = {},
) {
    val scope    = rememberCoroutineScope()
    var holdJob  by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var pressed  by remember { mutableStateOf(false) }

    // Breathing pulse animation for the outer ring
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

    val pressScale = if (pressed) 0.95f else 1f

    val pointerModifier = Modifier.pointerInput(holdDurationMs) {
        detectTapGestures(
            onPress = {
                pressed = true
                holdJob = scope.launch {
                    val tickMs = 50L
                    val ticks  = holdDurationMs / tickMs
                    for (i in 0..ticks) {
                        progress = i.toFloat() / ticks
                        delay(tickMs)
                    }
                    onActivated()
                    progress = 0f
                }
                tryAwaitRelease()
                pressed = false
                holdJob?.cancel()
                holdJob = null
                progress = 0f
            },
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier,
    ) {
        // ── Outer thick salmon/pink ring band ─────────────────────────────────
        Box(
            modifier = Modifier
                .size(246.dp)
                .scale(pulseScale)
                .background(
                    color = ResQOuterRing.copy(alpha = 0.55f),
                    shape = CircleShape,
                ),
        )

        // ── Core Red Button ───────────────────────────────────────────────────
        Surface(
            shape    = CircleShape,
            color    = ResQRed,
            modifier = Modifier
                .size(178.dp)
                .scale(pressScale)
                .shadow(
                    elevation    = 20.dp,
                    shape        = CircleShape,
                    spotColor    = ResQRed.copy(alpha = 0.45f),
                    ambientColor = ResQRed.copy(alpha = 0.2f),
                )
                .then(pointerModifier),
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

                    // "HOLD 4S" badge with dark red pill background (identik dengan gambar)
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
    }
}
