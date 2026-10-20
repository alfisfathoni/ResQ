package com.example.resq.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.model.NavItem
import com.example.resq.ui.theme.NavActiveRed
import com.example.resq.ui.theme.NavInactiveDark
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed

val DefaultNavItems = listOf(
    NavItem("home",    "Home",   Icons.Outlined.Home),
    NavItem("map",     "Map",    Icons.Outlined.LocationOn),
    NavItem("report",  "Report", Icons.Outlined.Assignment),
    NavItem("profile", "Profile", Icons.Outlined.Person),
)

/**
 * Curved Bottom Navigation Bar with a cradle dip in the center for the FAB:
 * - White background with a smooth scooped curve notch in the center.
 * - Central circular red floating action button (FAB) with Groups icon.
 * - Active tab: "Home" (Red icon + Red label).
 * - Other tabs: "Map", "Report", "Profile" (Dark gray icon + Dark gray label).
 */
@Composable
fun ResQBottomNavBar(
    selectedRoute : String           = "home",
    items         : List<NavItem>    = DefaultNavItems,
    onItemSelected: (String) -> Unit = {},
    onFabClick    : () -> Unit       = {},
    modifier      : Modifier         = Modifier,
) {
    val leftItems  = items.take(2)
    val rightItems = items.drop(2)

    Box(
        modifier         = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // ── Custom Curved Canvas Background ──────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .shadow(
                    elevation    = 12.dp,
                    spotColor    = Color.Black.copy(alpha = 0.10f),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                )
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cradleRadius = 46.dp.toPx()
            val cornerRadius = 24.dp.toPx()

            val path = Path().apply {
                moveTo(0f, cornerRadius)
                // Top-left rounded corner
                quadraticTo(0f, 0f, cornerRadius, 0f)

                // Line to start of center curve
                lineTo(cx - cradleRadius * 1.35f, 0f)

                // Smooth concave dip around the center FAB
                cubicTo(
                    cx - cradleRadius * 0.85f, 0f,
                    cx - cradleRadius * 0.85f, cradleRadius * 0.95f,
                    cx, cradleRadius * 0.95f,
                )
                cubicTo(
                    cx + cradleRadius * 0.85f, cradleRadius * 0.95f,
                    cx + cradleRadius * 0.85f, 0f,
                    cx + cradleRadius * 1.35f, 0f,
                )

                // Line to top-right corner
                lineTo(w - cornerRadius, 0f)
                // Top-right rounded corner
                quadraticTo(w, 0f, w, cornerRadius)

                // Right, bottom, and left edges
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(path = path, color = Color.White, style = Fill)
        }

        // ── Tab items row ────────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            // Left tabs: Home, Map
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                leftItems.forEach { item ->
                    NavTab(
                        item       = item,
                        isSelected = item.route == selectedRoute,
                        onClick    = { onItemSelected(item.route) },
                    )
                }
            }

            // Gap in the center for the FAB cradle
            Spacer(modifier = Modifier.size(72.dp))

            // Right tabs: Report, Profile
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                rightItems.forEach { item ->
                    NavTab(
                        item       = item,
                        isSelected = item.route == selectedRoute,
                        onClick    = { onItemSelected(item.route) },
                    )
                }
            }
        }

        // ── Floating Action Button (Centred in cradle) ─────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .size(64.dp)
                .shadow(
                    elevation    = 14.dp,
                    shape        = CircleShape,
                    spotColor    = ResQRed.copy(alpha = 0.5f),
                    ambientColor = ResQRed.copy(alpha = 0.2f),
                )
                .background(color = ResQRed, shape = CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onFabClick),
        ) {
            Icon(
                imageVector        = Icons.Rounded.Groups,
                contentDescription = "Community",
                tint               = Color.White,
                modifier           = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun NavTab(
    item      : NavItem,
    isSelected: Boolean,
    onClick   : () -> Unit,
) {
    Column(
        modifier            = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector        = if (isSelected) item.selectedIcon else item.icon,
            contentDescription = item.label,
            tint               = if (isSelected) NavActiveRed else NavInactiveDark,
            modifier           = Modifier.size(24.dp),
        )
        Text(
            text  = item.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PoppinsFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize   = 11.sp,
                color      = if (isSelected) NavActiveRed else NavInactiveDark,
            ),
        )
    }
}
