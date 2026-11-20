package com.example.resq.ui.screen.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.resq.ui.components.ResQBottomNavBar
import com.example.resq.ui.model.Community
import com.example.resq.ui.model.EmergencyContact
import com.example.resq.ui.theme.BackgroundLight
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Companion main screen — matches the leftmost design panel "Companion".
 *
 * Layout:
 *  - Search bar at top
 *  - "My Contacts" section: 3-item preview list + "View all" link
 *  - "My Communities" section: 2-column community card grid preview + "View all" link
 *  - Curved bottom nav bar (companion FAB selected/active)
 */

private sealed class CompanionSubScreen {
    object Main                                        : CompanionSubScreen()
    object AllContacts                                 : CompanionSubScreen()
    object AllCommunities                              : CompanionSubScreen()
    data class CommunityDetail(val community: Community) : CompanionSubScreen()
    object CreateCommunity                             : CompanionSubScreen()
}

@Composable
fun CompanionScreen(
    contacts            : List<EmergencyContact>,
    communities         : List<Community>,
    onAddContact        : (name: String, relationship: String, phone: String) -> Unit,
    onCreateCommunity   : (name: String, memberIds: List<Int>) -> Unit,
    onUpdateCommunityMembers: (communityId: Int, memberIds: List<Int>) -> Unit,
    onNavItemSelected   : (String) -> Unit = {},
    onFabClick          : () -> Unit       = {},
) {
    var subScreen by remember { mutableStateOf<CompanionSubScreen>(CompanionSubScreen.Main) }

    // Route sub-screens — back always returns to Main (or previous)
    when (val screen = subScreen) {
        is CompanionSubScreen.AllContacts -> {
            ViewAllContactsScreen(
                contacts     = contacts,
                onAddContact = onAddContact,
                onBack       = { subScreen = CompanionSubScreen.Main },
            )
            return
        }
        is CompanionSubScreen.AllCommunities -> {
            ViewAllCommunitiesScreen(
                communities      = communities,
                allContacts      = contacts,
                onCommunityClick = { community -> subScreen = CompanionSubScreen.CommunityDetail(community) },
                onCreateCommunity = { name, memberIds ->
                    onCreateCommunity(name, memberIds)
                    subScreen = CompanionSubScreen.Main
                },
                onBack = { subScreen = CompanionSubScreen.Main },
            )
            return
        }
        is CompanionSubScreen.CommunityDetail -> {
            // Always fetch latest community state (after member updates)
            val latest = communities.find { it.id == screen.community.id } ?: screen.community
            CommunityDetailScreen(
                community       = latest,
                allContacts     = contacts,
                onUpdateMembers = { ids -> onUpdateCommunityMembers(latest.id, ids) },
                onBack          = { subScreen = CompanionSubScreen.AllCommunities },
            )
            return
        }
        is CompanionSubScreen.CreateCommunity -> {
            CreateCommunityScreen(
                allContacts   = contacts,
                onCreateClick = { name, memberIds ->
                    onCreateCommunity(name, memberIds)
                    subScreen = CompanionSubScreen.Main
                },
                onBack = { subScreen = CompanionSubScreen.Main },
            )
            return
        }
        is CompanionSubScreen.Main -> { /* render below */ }
    }

    // ── Main Companion Screen ──────────────────────────────────────────────────
    Box(
        modifier         = Modifier.fillMaxSize().background(BackgroundLight),
        contentAlignment = Alignment.BottomCenter,
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(52.dp))

                // ── Title ──────────────────────────────────────────────────
                Text(
                    text       = "Companion",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 22.sp,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = 20.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Search bar ─────────────────────────────────────────────
                OutlinedTextField(
                    value         = "",
                    onValueChange = {},
                    placeholder   = { Text("Search", fontFamily = PoppinsFamily, fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.6f)) },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                    enabled       = false, // tap navigates to respective list
                    singleLine    = true,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { subScreen = CompanionSubScreen.AllContacts },
                    shape         = RoundedCornerShape(50.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor    = Color(0xFFE5E7EB),
                        disabledContainerColor = Color.White,
                        disabledTextColor      = TextSecondary,
                        disabledPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
                        disabledLeadingIconColor = TextSecondary,
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = PoppinsFamily, fontSize = 13.sp),
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── My Contacts Section ────────────────────────────────────────
            item {
                SectionHeader(
                    title       = "My Contacts",
                    onViewAll   = { subScreen = CompanionSubScreen.AllContacts },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (contacts.isEmpty()) {
                item {
                    Text(
                        text       = "Belum ada kontak. Tambah dari Profile.",
                        fontFamily = PoppinsFamily,
                        fontSize   = 13.sp,
                        color      = TextSecondary,
                        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(contacts.take(3), key = { "c_${it.id}" }) { contact ->
                    ContactRow(
                        contact = contact,
                        onClick = { subScreen = CompanionSubScreen.AllContacts },
                    )
                }
            }

            // ── My Communities Section ─────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader(
                    title     = "My Communities",
                    onViewAll = { subScreen = CompanionSubScreen.AllCommunities },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (communities.isEmpty()) {
                item {
                    Text(
                        text       = "Belum ada komunitas. Buat komunitas pertama!",
                        fontFamily = PoppinsFamily,
                        fontSize   = 13.sp,
                        color      = TextSecondary,
                        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else {
                item {
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(communities.take(4), key = { "com_${it.id}" }) { community ->
                            CompactCommunityCard(
                                community   = community,
                                allContacts = contacts,
                                onClick     = { subScreen = CompanionSubScreen.CommunityDetail(community) },
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom Nav Bar ─────────────────────────────────────────────────
        ResQBottomNavBar(
            selectedRoute  = "companion",
            onItemSelected = onNavItemSelected,
            onFabClick     = onFabClick,
            modifier       = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ── Reusable sub-composables ──────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, onViewAll: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = title,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp,
            color      = TextPrimary,
        )
        Text(
            text       = "View all",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp,
            color      = ResQRed,
            modifier   = Modifier.clickable(onClick = onViewAll),
        )
    }
}

@Composable
private fun ContactRow(
    contact: EmergencyContact,
    onClick: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialAvatar(name = contact.name, size = 42.dp, fontSize = 15.sp)
        Text(
            text       = contact.name,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp,
            color      = TextPrimary,
        )
    }
}

@Composable
private fun CompactCommunityCard(
    community   : Community,
    allContacts : List<EmergencyContact>,
    onClick     : () -> Unit,
) {
    val members = allContacts.filter { it.id in community.memberIds }
    val extras  = (community.memberIds.size - 3).coerceAtLeast(0)

    Column(
        modifier            = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Stacked avatar cluster
        Box(modifier = Modifier.height(44.dp).fillMaxWidth()) {
            members.take(3).forEachIndexed { index, contact ->
                InitialAvatar(
                    name     = contact.name,
                    size     = 34.dp,
                    fontSize = 12.sp,
                    modifier = Modifier.offset(x = (index * 18).dp),
                )
            }
            if (extras > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .offset(x = (members.take(3).size * 18).dp)
                        .size(34.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color(0xFFE5E7EB)),
                ) {
                    Text(
                        text       = "+$extras",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 10.sp,
                        color      = TextSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text       = community.name,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 13.sp,
            color      = TextPrimary,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
        )

        Text(
            text       = "${community.memberIds.size} member",
            fontFamily = PoppinsFamily,
            fontSize   = 11.sp,
            color      = TextSecondary,
        )
    }
}
