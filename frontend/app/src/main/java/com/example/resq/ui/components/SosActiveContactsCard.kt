package com.example.resq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary

/**
 * Emergency contacts notification card for "SOS Aktif" screen:
 * - 3 overlapping user avatar circles on top.
 * - Rounded card below with:
 *   "Contact sent to 3 emergency contacts at [time]" (with time in bold red).
 */
@Composable
fun SosActiveContactsCard(
    modifier: Modifier = Modifier,
    sentTime: String   = "09:41",
) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── 3 Overlapping Avatars ─────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.zIndex(2f),
        ) {
            // Avatar 1
            ContactAvatar(
                gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                modifier       = Modifier.offset(x = 10.dp),
            )
            // Avatar 2 (Center)
            ContactAvatar(
                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                modifier       = Modifier.zIndex(3f),
            )
            // Avatar 3
            ContactAvatar(
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857)),
                modifier       = Modifier.offset(x = (-10).dp),
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // ── White Info Card ───────────────────────────────────────────────────
        Card(
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border    = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(Color(0xFFEEEEEE), Color(0xFFE5E5E5))),
            ),
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Contact sent to 3 emergency\ncontacts at ")
                    withStyle(
                        SpanStyle(
                            color      = ResQRed,
                            fontWeight = FontWeight.Bold,
                        )
                    ) {
                        append(sentTime)
                    }
                },
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    color      = TextPrimary,
                    fontSize   = 12.sp,
                    lineHeight = 17.sp,
                ),
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun ContactAvatar(
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .border(2.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .background(Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = Icons.Default.Person,
            contentDescription = "Contact",
            tint               = Color.White.copy(alpha = 0.9f),
            modifier           = Modifier.size(20.dp),
        )
    }
}
