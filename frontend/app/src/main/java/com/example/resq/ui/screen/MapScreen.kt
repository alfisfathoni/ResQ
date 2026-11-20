package com.example.resq.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.components.CurrentAddressCard
import com.example.resq.ui.components.ResQBottomNavBar
import com.example.resq.ui.components.ResQLogoHeader
import com.example.resq.ui.theme.BackgroundLight
import com.example.resq.ui.theme.BadgeGreenSolid
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Screen for "Map" tab with active navbar routing.
 */
@Composable
fun MapScreen(
    onNavItemSelected   : (String) -> Unit = {},
    onCommunityFabClick : () -> Unit       = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Header Logo
            ResQLogoHeader(
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Map Preview Card
            Card(
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Default.LocationOn,
                                contentDescription = "Radar",
                                tint               = ResQRed,
                                modifier           = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text       = "Radar Tanggap Darurat",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 14.sp,
                                color      = TextPrimary,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(BadgeGreenSolid.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text       = "GPS LIVE",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 10.sp,
                                color      = BadgeGreenSolid,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Map View Simulation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                                )
                            )
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        // User Location Marker
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(ResQRed.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(ResQRed, CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Navigation,
                                    contentDescription = "User Location",
                                    tint               = Color.White,
                                    modifier           = Modifier.size(12.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Nearby Responders Count
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        NearbyResponderChip(
                            icon  = Icons.Default.LocalHospital,
                            label = "Ambulans (800m)",
                            color = Color(0xFFE53935),
                        )
                        NearbyResponderChip(
                            icon  = Icons.Default.LocalPolice,
                            label = "Polisi (1.2km)",
                            color = Color(0xFF1E88E5),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Current Address Card
            CurrentAddressCard(
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }

        // ── Pinned Curved Bottom Nav Bar (Active on "map") ───────────────────
        ResQBottomNavBar(
            selectedRoute  = "map",
            onItemSelected = onNavItemSelected,
            onFabClick     = onCommunityFabClick,
            modifier       = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun NearbyResponderChip(
    icon  : androidx.compose.ui.graphics.vector.ImageVector,
    label : String,
    color : Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = color,
            modifier           = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text       = label,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Medium,
            fontSize   = 11.sp,
            color      = color,
        )
    }
}
