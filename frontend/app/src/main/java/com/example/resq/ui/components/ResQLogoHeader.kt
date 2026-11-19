package com.example.resq.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * RESQ Logo Header matching the reference screenshots:
 * - Red bold "RESQ" brand text with outline location pin icon on "Q".
 * - In normal state: 2-line instruction subtitle in subtle gray.
 * - In countdown/holding state: "RELEASE TO CANCEL" in bold dark text.
 * - In SOS active state: subtitle is hidden ([showSubtitle] = false).
 */
@Composable
fun ResQLogoHeader(
    modifier       : Modifier = Modifier,
    isCountingDown : Boolean  = false,
    showSubtitle   : Boolean  = true,
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // ── Logo "RESQ" + outline pin icon at top-right of "Q" ───────────────
        Row(
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color         = ResQRed,
                            fontSize      = 40.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            fontFamily    = PoppinsFamily,
                            letterSpacing = (-0.5).sp,
                        )
                    ) {
                        append("RESQ")
                    }
                },
            )

            // Outline location pin icon at top right
            Icon(
                imageVector        = Icons.Outlined.LocationOn,
                contentDescription = "Pin",
                tint               = ResQRed,
                modifier           = Modifier
                    .size(20.dp)
                    .padding(top = 4.dp),
            )
        }

        // ── Subtitle text ─────────────────────────────────────────────────────
        if (showSubtitle) {
            if (isCountingDown) {
                Text(
                    text = "RELEASE TO CANCEL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily    = PoppinsFamily,
                        fontWeight    = FontWeight.Bold,
                        color         = TextPrimary,
                        fontSize      = 14.sp,
                        letterSpacing = 0.5.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = "Press the button below 3 second and emergency\nresponders will reach you immediately",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Normal,
                        color      = TextSecondary,
                        fontSize   = 12.sp,
                        lineHeight = 18.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
