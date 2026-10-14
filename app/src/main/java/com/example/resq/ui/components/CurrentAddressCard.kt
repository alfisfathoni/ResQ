package com.example.resq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FmdGood
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.theme.BadgeGreenSolid
import com.example.resq.ui.theme.BadgeRedSolid
import com.example.resq.ui.theme.DividerColor
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * "Your Current Address" info card.
 * Exact match to screenshot:
 * - Red location pin icon on left
 * - Header "YOUR CURRENT ADDRESS" with small green live indicator dot
 * - Bold street address: "Jl. Jend. Sudirman No. 42"
 * - Subtitle: "Senayan, Kebayoran Baru, Jakarta Selatan"
 * - Bottom row:
 *   - "Precision: " + solid green badge "HIGH (3 METER)" with white text
 *   - "Dispatched: " + solid red badge "0 UNITS" with white text
 */
@Composable
fun CurrentAddressCard(
    modifier      : Modifier = Modifier,
    streetAddress : String   = "Jl. Jend. Sudirman No. 42",
    subAddress    : String   = "Senayan, Kebayoran Baru, Jakarta Selatan",
    precisionLabel: String   = "HIGH (3 METER)",
    dispatchLabel : String   = "0 UNITS",
) {
    Card(
        modifier  = modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 6.dp,
                shape        = RoundedCornerShape(20.dp),
                spotColor    = Color.Black.copy(alpha = 0.08f),
                ambientColor = Color.Black.copy(alpha = 0.04f),
            ),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column {
            // ── Top: Pin Icon & Address details ───────────────────────────────
            Row(
                modifier          = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Red pin icon
                Icon(
                    imageVector        = Icons.Rounded.FmdGood,
                    contentDescription = "Location",
                    tint               = ResQRed,
                    modifier           = Modifier
                        .size(32.dp)
                        .padding(top = 2.dp),
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    // Header row: "YOUR CURRENT ADDRESS" + green dot
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text  = "YOUR CURRENT ADDRESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily    = PoppinsFamily,
                                fontWeight    = FontWeight.Bold,
                                fontSize      = 10.sp,
                                letterSpacing = 0.6.sp,
                                color         = TextSecondary,
                            ),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(color = BadgeGreenSolid, shape = CircleShape),
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Street Name (Bold)
                    Text(
                        text  = streetAddress,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp,
                            color      = TextPrimary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Sub-address details
                    Text(
                        text  = subAddress,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 11.sp,
                            color      = TextSecondary,
                        ),
                    )
                }
            }

            // Divider line
            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            // ── Bottom badges row ─────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Precision badge (Green solid)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "Precision: ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PoppinsFamily,
                            color      = TextSecondary,
                            fontSize   = 10.sp,
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text     = precisionLabel,
                        color    = Color.White,
                        style    = MaterialTheme.typography.labelSmall.copy(
                            fontFamily    = PoppinsFamily,
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 9.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        modifier = Modifier
                            .background(color = BadgeGreenSolid, shape = RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }

                // Dispatch badge (Red solid)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "Dispatched: ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PoppinsFamily,
                            color      = TextSecondary,
                            fontSize   = 10.sp,
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text     = dispatchLabel,
                        color    = Color.White,
                        style    = MaterialTheme.typography.labelSmall.copy(
                            fontFamily    = PoppinsFamily,
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 9.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        modifier = Modifier
                            .background(color = BadgeRedSolid, shape = RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}
