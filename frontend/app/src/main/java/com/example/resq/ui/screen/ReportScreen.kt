package com.example.resq.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.components.ResQBottomNavBar
import com.example.resq.ui.components.ResQLogoHeader
import com.example.resq.ui.theme.BackgroundLight
import com.example.resq.ui.theme.BadgeGreenSolid
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Screen for "Report" tab with active navbar routing.
 */
@Composable
fun ReportScreen(
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

            // Incident History Card
            Card(
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Default.Assignment,
                            contentDescription = "Report",
                            tint               = ResQRed,
                            modifier           = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text       = "Riwayat & Laporan Darurat",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp,
                            color      = TextPrimary,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Item 1
                    ReportHistoryItem(
                        title  = "Panggilan Darurat 112 (Demo)",
                        date   = "Hari ini, 09:41",
                        status = "Terkirim",
                        color  = BadgeGreenSolid,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Item 2
                    ReportHistoryItem(
                        title  = "Notifikasi Kontak Darurat (3 Kontak)",
                        date   = "Hari ini, 09:41",
                        status = "Tersampaikan",
                        color  = BadgeGreenSolid,
                    )
                }
            }
        }

        // ── Pinned Curved Bottom Nav Bar (Active on "report") ────────────────
        ResQBottomNavBar(
            selectedRoute  = "report",
            onItemSelected = onNavItemSelected,
            onFabClick     = onCommunityFabClick,
            modifier       = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ReportHistoryItem(
    title  : String,
    date   : String,
    status : String,
    color  : Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 12.sp,
                color      = TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text       = date,
                fontFamily = PoppinsFamily,
                fontSize   = 10.sp,
                color      = TextSecondary,
            )
        }

        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text       = status,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize   = 10.sp,
                color      = color,
            )
        }
    }
}
