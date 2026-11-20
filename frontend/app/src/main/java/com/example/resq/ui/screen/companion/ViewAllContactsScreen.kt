package com.example.resq.ui.screen.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.components.InitialAvatar
import com.example.resq.ui.model.EmergencyContact
import com.example.resq.ui.theme.BackgroundLight
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Full contact list screen: "My Contacts" with A-Z grouping,
 * alphabetical index scrubber on the right, and FAB to add new contact.
 */
@Composable
fun ViewAllContactsScreen(
    contacts    : List<EmergencyContact>,
    onAddContact: (name: String, relationship: String, phone: String) -> Unit,
    onBack      : () -> Unit,
) {
    var query          by remember { mutableStateOf("") }
    var showAddSheet   by remember { mutableStateOf(false) }

    val filtered = remember(query, contacts) {
        if (query.isBlank()) contacts
        else contacts.filter { it.name.contains(query, ignoreCase = true) }
    }

    val grouped: Map<Char, List<EmergencyContact>> = remember(filtered) {
        filtered.sortedBy { it.name.uppercase() }
            .groupBy { it.name.trim().firstOrNull()?.uppercaseChar() ?: '#' }
            .toSortedMap()
    }
    val letters = grouped.keys.toList()

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
                    text       = "My Contacts",
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

            Spacer(modifier = Modifier.height(8.dp))

            if (contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada kontak.\nTambah kontak pertamamu!",
                        color      = TextSecondary,
                        fontFamily = PoppinsFamily,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                // ── A-Z list + index ───────────────────────────────────────
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize().padding(end = 24.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                    ) {
                        grouped.forEach { (letter, group) ->
                            item(key = "h_$letter") {
                                Text(
                                    text       = letter.toString(),
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 13.sp,
                                    color      = TextSecondary,
                                    modifier   = Modifier
                                        .fillMaxWidth()
                                        .background(BackgroundLight)
                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                )
                            }
                            items(group, key = { it.id }) { contact ->
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .shadow(1.dp, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    InitialAvatar(name = contact.name, size = 42.dp, fontSize = 15.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text       = contact.name,
                                            fontFamily = PoppinsFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize   = 14.sp,
                                            color      = TextPrimary,
                                        )
                                        Text(
                                            text       = contact.relationship,
                                            fontFamily = PoppinsFamily,
                                            fontSize   = 12.sp,
                                            color      = TextSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Alphabetical scrubber ──────────────────────────────
                    Column(
                        modifier            = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        letters.forEach { letter ->
                            Text(
                                text       = letter.toString(),
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 10.sp,
                                color      = ResQRed,
                            )
                        }
                    }

                    // ── FAB + ──────────────────────────────────────────────
                    FloatingActionButton(
                        onClick           = { showAddSheet = true },
                        containerColor    = ResQRed,
                        contentColor      = Color.White,
                        shape             = CircleShape,
                        modifier          = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 24.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah kontak")
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddContactSheet(
            onSave    = { name, rel, phone ->
                onAddContact(name, rel, phone)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false },
        )
    }
}
