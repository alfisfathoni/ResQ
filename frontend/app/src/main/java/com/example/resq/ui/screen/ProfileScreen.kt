package com.example.resq.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.components.ResQBottomNavBar
import com.example.resq.ui.model.EmergencyContact
import com.example.resq.ui.theme.BackgroundLight
import com.example.resq.ui.theme.BadgeGreenSolid
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Profile Screen matching the reference design:
 * - Red curved header with avatar & online badge.
 * - Profile card: Name, Email, Phone, and Edit button.
 * - Emergency Contact (SOS) section with dynamic list:
 *   shows contacts entered during registration / onboarding, or empty state if none yet.
 * - Security and Verification card (PIN).
 * - Help & Support card with 3 action items.
 * - Red Logout button at the bottom.
 * - Pinned Bottom Navigation Bar.
 */
@Composable
fun ProfileScreen(
    userName          : String                 = "ForestGump",
    userEmail         : String                 = "gumpfor@gmail.com",
    userPhone         : String                 = "+62 89507274535",
    emergencyContacts : List<EmergencyContact> = emptyList(),
    onAddContactClick : () -> Unit             = {},
    onNavigateToHome  : () -> Unit             = {},
    onLogout          : () -> Unit             = {},
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp),
        ) {
            // ── 1. Top Red Header & User Info Card ────────────────────────────
            ProfileHeaderSection(
                userName  = userName,
                userEmail = userEmail,
                userPhone = userPhone,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── 2. Emergency Contact (SOS) Card ───────────────────────────────
            EmergencyContactSection(
                contacts          = emergencyContacts,
                onAddContactClick = onAddContactClick,
                onCallContact     = { phone ->
                    try {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phone")
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                modifier          = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 3. Security and Verification Card ─────────────────────────────
            SecurityVerificationSection(
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 4. Help & Support Card ────────────────────────────────────────
            HelpAndSupportSection(
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── 5. Logout Button ──────────────────────────────────────────────
            Button(
                onClick  = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = ResQRed,
                    contentColor   = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text       = "Logout",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Pinned Curved Bottom Nav Bar ──────────────────────────────────────
        ResQBottomNavBar(
            selectedRoute  = "profile",
            onItemSelected = { route ->
                if (route == "home") {
                    onNavigateToHome()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeaderSection(
    userName  : String,
    userEmail : String,
    userPhone : String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
    ) {
        // Red Curved Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFEB2A2E), Color(0xFFD01B20))
                    )
                ),
        )

        // Floating User Profile Card Overlapping Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: Circular Avatar with Online Badge
            Box(
                modifier = Modifier.size(92.dp),
            ) {
                Surface(
                    shape           = CircleShape,
                    color           = Color(0xFFE2E8F0),
                    shadowElevation = 6.dp,
                    modifier        = Modifier
                        .size(88.dp)
                        .border(3.dp, Color.White, CircleShape),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF334155), Color(0xFF0F172A))
                                )
                            ),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint               = Color.White,
                            modifier           = Modifier.size(54.dp),
                        )
                    }
                }

                // Green Online Indicator Badge
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(2.5.dp, Color.White, CircleShape)
                        .background(BadgeGreenSolid, CircleShape)
                        .align(Alignment.BottomEnd),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Right: User Details Card
            Card(
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier  = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                ) {
                    Text(
                        text          = userName,
                        fontFamily    = PoppinsFamily,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 16.sp,
                        color         = TextPrimary,
                        maxLines      = 1,
                        overflow      = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Email
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Default.Email,
                            contentDescription = null,
                            tint               = TextSecondary,
                            modifier           = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text       = userEmail,
                            fontFamily = PoppinsFamily,
                            fontSize   = 11.sp,
                            color      = TextSecondary,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Phone
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Default.Phone,
                            contentDescription = null,
                            tint               = TextSecondary,
                            modifier           = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text       = userPhone,
                            fontFamily = PoppinsFamily,
                            fontSize   = 11.sp,
                            color      = TextSecondary,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Red Edit Button
                    Button(
                        onClick        = { },
                        modifier       = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        shape          = RoundedCornerShape(8.dp),
                        colors         = ButtonDefaults.buttonColors(
                            containerColor = ResQRed,
                            contentColor   = Color.White,
                        ),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint               = Color.White,
                            modifier           = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text       = "Edit",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyContactSection(
    contacts          : List<EmergencyContact>,
    onAddContactClick : () -> Unit,
    onCallContact     : (phone: String) -> Unit,
    modifier          : Modifier = Modifier,
) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Default.People,
                        contentDescription = "Emergency Contacts",
                        tint               = ResQRed,
                        modifier           = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text       = "Emergency Contact\n(SOS)",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                        color      = TextPrimary,
                        lineHeight = 17.sp,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge count "X/3"
                    Box(
                        modifier = Modifier
                            .background(
                                color = ResQRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text       = "${contacts.size}/3",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 11.sp,
                            color      = ResQRed,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // "+ Tambah" Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ResQRed)
                            .clickable { onAddContactClick() }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = "+ Tambah",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 11.sp,
                            color      = Color.White,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contact List or Empty State
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9FAFB))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "Belum ada kontak darurat yang tersimpan.\nTambahkan melalui formulir saat registrasi atau tombol '+ Tambah' di atas.",
                        fontFamily = PoppinsFamily,
                        fontSize   = 11.sp,
                        color      = TextSecondary,
                        textAlign  = TextAlign.Center,
                        lineHeight = 16.sp,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    contacts.forEachIndexed { index, contact ->
                        ContactItemRow(
                            index         = index + 1,
                            name          = contact.name,
                            phone         = contact.phone,
                            onCallClicked = { onCallContact(contact.phone) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactItemRow(
    index         : Int,
    name          : String,
    phone         : String,
    onCallClicked : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.weight(1f),
        ) {
            // Number Circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = index.toString(),
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 12.sp,
                    color      = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text       = name,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    text       = phone,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 11.sp,
                    color      = TextSecondary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Green Pill Badge "Auto Telp/SMS"
            Box(
                modifier = Modifier
                    .background(BadgeGreenSolid, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "Auto Telp/SMS",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 9.sp,
                    color      = Color.White,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // White Circular Call Button
            Surface(
                shape           = CircleShape,
                color           = Color.White,
                shadowElevation = 1.dp,
                modifier        = Modifier
                    .size(26.dp)
                    .clickable { onCallClicked() },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier.fillMaxSize(),
                ) {
                    Icon(
                        imageVector        = Icons.Default.Call,
                        contentDescription = "Panggil",
                        tint               = ResQRed,
                        modifier           = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityVerificationSection(modifier: Modifier = Modifier) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.Shield,
                    contentDescription = null,
                    tint               = ResQRed,
                    modifier           = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text       = "Security and\nVerification",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                    lineHeight = 17.sp,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PIN Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6))
                    .clickable { }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFFFEAEA), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Pin,
                            contentDescription = "PIN",
                            tint               = ResQRed,
                            modifier           = Modifier.size(18.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text       = "PIN",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                            color      = TextPrimary,
                        )
                        Text(
                            text       = "Secure your medical info & contacts",
                            fontFamily = PoppinsFamily,
                            fontSize   = 11.sp,
                            color      = TextSecondary,
                        )
                    }
                }

                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Go",
                    tint               = TextSecondary,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun HelpAndSupportSection(modifier: Modifier = Modifier) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint               = ResQRed,
                    modifier           = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text       = "Help & Support",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(10.dp))

            // 3 Action items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HelpItemRow(
                    icon  = Icons.Default.PlayCircleOutline,
                    title = "SOS Operational Guide",
                )
                HelpItemRow(
                    icon  = Icons.Default.HeadsetMic,
                    title = "Help Center & Emergency CS",
                )
                HelpItemRow(
                    icon  = Icons.Default.Policy,
                    title = "Privacy Policy & Terms of Service",
                )
            }
        }
    }
}

@Composable
private fun HelpItemRow(
    icon  : ImageVector,
    title : String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F4F6))
            .clickable { }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = ResQRed,
                modifier           = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text       = title,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize   = 12.sp,
                color      = TextPrimary,
            )
        }

        Icon(
            imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint               = TextSecondary,
            modifier           = Modifier.size(18.dp),
        )
    }
}
