package com.example.resq.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.theme.PoppinsFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tampilan Depan (Splash Screen) aplikasi RESQ.
 * Sesuai desain referensi "Depan":
 * - Fullscreen background merah gradien halus.
 * - Logo "RESQ" putih bold di tengah layar.
 * - Icon outline location pin putih di sudut kanan atas huruf "Q".
 * - Transisi otomatis ke Home (jika sudah login/register) atau ke Login (jika belum).
 */
@Composable
fun SplashScreen(
    durationMillis: Long = 2000L,
    onSplashFinished: () -> Unit,
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.92f) }

    LaunchedEffect(Unit) {
        // Animasi fade in & scale halus
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            )
        }

        // Tahan sebentar sesuai durasi splash, lalu berpindah layar
        delay(durationMillis)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEB2A2E), // Merah atas
                        Color(0xFFE5252A), // Merah tengah
                        Color(0xFFD01B20), // Merah bawah sedikit lebih gelap
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Logo Brand RESQ di tengah
        Row(
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "RESQ",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PoppinsFamily,
                letterSpacing = (-0.5).sp,
            )

            // Outline location pin icon di kanan atas huruf 'Q'
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = "Location Pin",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 4.dp),
            )
        }
    }
}
