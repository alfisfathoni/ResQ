package com.example.resq.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.model.ResponderItem
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResponderFamily
import com.example.resq.ui.theme.ResponderFire
import com.example.resq.ui.theme.ResponderMedical
import com.example.resq.ui.theme.ResponderPolice
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * 4 Instant Responders dengan ikon dan warna masing-masing yang sesuai gambar:
 * 1. Firefighter -> Orange flame icon
 * 2. Ambulance -> Red medical kit icon
 * 3. Police -> Blue siren/beacon icon
 * 4. Family -> Brown/taupe family/people icon
 */
val defaultResponders = listOf(
    ResponderItem("Firefighter", Icons.Rounded.LocalFireDepartment, ResponderFire,    "Call Firefighter"),
    ResponderItem("Ambulance",   Icons.Rounded.MedicalServices,     ResponderMedical, "Call Ambulance"),
    ResponderItem("Police",      Icons.Rounded.NotificationsActive, ResponderPolice,  "Call Police"),
    ResponderItem("Family",      Icons.Rounded.Groups,              ResponderFamily,  "Alert Family"),
)

@Composable
fun InstantRespondersSection(
    modifier        : Modifier                = Modifier,
    responders      : List<ResponderItem>     = defaultResponders,
    onResponderClick: (ResponderItem) -> Unit = {},
) {
    Column(modifier = modifier) {
        // ── Section Title ─────────────────────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = "INSTANT RESPONDERS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily    = PoppinsFamily,
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = 15.sp,
                    letterSpacing = 1.2.sp,
                    color         = TextPrimary,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = "Hold 3S",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    color      = TextSecondary,
                    fontSize   = 11.sp,
                ),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── 4 Horizontal Cards ────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            responders.forEach { responder ->
                ResponderCard(
                    item     = responder,
                    onClick  = { onResponderClick(responder) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ResponderCard(
    item    : ResponderItem,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .shadow(
                elevation    = 6.dp,
                shape        = RoundedCornerShape(18.dp),
                spotColor    = Color.Black.copy(alpha = 0.08f),
                ambientColor = Color.Black.copy(alpha = 0.04f),
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Icon dengan warna spesifik per kategori (bersih tanpa bulatan latar)
            Icon(
                imageVector        = item.icon,
                contentDescription = item.contentDescription,
                tint               = item.tintColor,
                modifier           = Modifier.size(28.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Label
            Text(
                text      = item.label,
                style     = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 11.sp,
                    color      = TextPrimary,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}
