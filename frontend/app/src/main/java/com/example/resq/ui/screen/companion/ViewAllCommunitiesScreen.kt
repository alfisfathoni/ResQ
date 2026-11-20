package com.example.resq.ui.screen.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.components.InitialAvatar
import com.example.resq.ui.model.Community
import com.example.resq.ui.model.EmergencyContact
import com.example.resq.ui.theme.BackgroundLight
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Grid view of all communities with search bar, back button,
 * and FAB to create a new community.
 */
@Composable
fun ViewAllCommunitiesScreen(
    communities     : List<Community>,
    allContacts     : List<EmergencyContact>,
    onCommunityClick: (Community) -> Unit,
    onCreateCommunity: (name: String, memberIds: List<Int>) -> Unit,
    onBack          : () -> Unit,
) {
    var query            by remember { mutableStateOf("") }
    var showCreateScreen by remember { mutableStateOf(false) }

    if (showCreateScreen) {
        CreateCommunityScreen(
            allContacts   = allContacts,
            onCreateClick = { name, memberIds ->
                onCreateCommunity(name, memberIds)
                showCreateScreen = false
            },
            onBack        = { showCreateScreen = false },
        )
        return
    }

    val filtered = remember(query, communities) {
        if (query.isBlank()) communities
        else communities.filter { it.name.contains(query, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Top bar ────────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
                Text(
                    text       = "My Communities",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Search bar ─────────────────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("Search", fontFamily = PoppinsFamily, fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.6f)) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape         = RoundedCornerShape(50.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = ResQRed,
                    unfocusedBorderColor    = Color(0xFFE5E7EB),
                    unfocusedContainerColor = BackgroundLight,
                    focusedContainerColor   = BackgroundLight,
                    cursorColor             = ResQRed,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = PoppinsFamily, fontSize = 13.sp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text       = if (communities.isEmpty()) "Belum ada komunitas.\nBuat komunitas pertamamu!" else "Tidak ada komunitas ditemukan",
                        color      = TextSecondary,
                        fontFamily = PoppinsFamily,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                // ── 2-column community grid ────────────────────────────────
                LazyVerticalGrid(
                    columns            = GridCells.Fixed(2),
                    modifier           = Modifier.fillMaxSize(),
                    contentPadding     = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.id }) { community ->
                        CommunityCard(
                            community   = community,
                            allContacts = allContacts,
                            onClick     = { onCommunityClick(community) },
                        )
                    }
                    // Extra spacer at bottom for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // ── FAB + ──────────────────────────────────────────────────────────
        FloatingActionButton(
            onClick        = { showCreateScreen = true },
            containerColor = ResQRed,
            contentColor   = Color.White,
            shape          = CircleShape,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Buat komunitas")
        }
    }
}

/**
 * Card for a single community in the grid.
 * Shows group avatar (stacked initials of first 3 members) + name + member count badge.
 */
@Composable
private fun CommunityCard(
    community   : Community,
    allContacts : List<EmergencyContact>,
    onClick     : () -> Unit,
) {
    val members = allContacts.filter { it.id in community.memberIds }
    val extras  = (community.memberIds.size - 3).coerceAtLeast(0)

    Column(
        modifier            = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Stacked avatar cluster
        Box(modifier = Modifier.height(44.dp)) {
            members.take(3).forEachIndexed { index, contact ->
                InitialAvatar(
                    name     = contact.name,
                    size     = 36.dp,
                    fontSize = 13.sp,
                    modifier = Modifier.offset(x = (index * 20).dp),
                )
            }
            if (extras > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .offset(x = (members.take(3).size * 20).dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB)),
                ) {
                    Text(
                        text       = "+$extras",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 11.sp,
                        color      = TextSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text       = community.name,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 14.sp,
            color      = TextPrimary,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
        )

        Text(
            text       = "${community.memberIds.size} member",
            fontFamily = PoppinsFamily,
            fontSize   = 12.sp,
            color      = TextSecondary,
        )
    }
}
