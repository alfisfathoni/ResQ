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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Screen to select contacts to add into a community group.
 * Shows A-Z grouped contact list with alphabetical index scrubber on the right.
 * Pre-selects already-added members (selectedIds).
 */
@Composable
fun SelectToGroupScreen(
    allContacts : List<EmergencyContact>,
    selectedIds : Set<Int>                        = emptySet(),
    onConfirm   : (List<EmergencyContact>) -> Unit,
    onBack      : () -> Unit,
) {
    var query    by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(selectedIds.toMutableSet()) }

    val filtered = remember(query, allContacts) {
        if (query.isBlank()) allContacts
        else allContacts.filter { it.name.contains(query, ignoreCase = true) }
    }

    // Group by first letter
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
                    text       = "Select",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = TextPrimary,
                    modifier   = Modifier.weight(1f).padding(start = 4.dp),
                )
                // Confirm button — shown when at least one selected
                if (selected.isNotEmpty()) {
                    TextButton(onClick = {
                        onConfirm(allContacts.filter { it.id in selected })
                    }) {
                        Text(
                            text       = "Done (${selected.size})",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            color      = ResQRed,
                        )
                    }
                }
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
                    focusedBorderColor   = ResQRed,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    unfocusedContainerColor = BackgroundLight,
                    focusedContainerColor   = BackgroundLight,
                    cursorColor          = ResQRed,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = PoppinsFamily, fontSize = 13.sp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada kontak ditemukan", color = TextSecondary, fontFamily = PoppinsFamily)
                }
            } else {
                // ── A-Z list + index ───────────────────────────────────────
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(end = 24.dp),
                        contentPadding      = PaddingValues(bottom = 16.dp),
                    ) {
                        grouped.forEach { (letter, contacts) ->
                            item(key = "header_$letter") {
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
                            items(contacts, key = { it.id }) { contact ->
                                val isSelected = contact.id in selected
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected = selected.toMutableSet().apply {
                                                if (isSelected) remove(contact.id) else add(contact.id)
                                            }
                                        }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    InitialAvatar(name = contact.name, size = 42.dp, fontSize = 15.sp)
                                    Text(
                                        text       = contact.name,
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize   = 14.sp,
                                        color      = TextPrimary,
                                        modifier   = Modifier.weight(1f),
                                    )
                                    if (isSelected) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier         = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(ResQRed),
                                        ) {
                                            Icon(
                                                imageVector        = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint               = Color.White,
                                                modifier           = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Alphabetical index scrubber ────────────────────────
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
                }
            }
        }
    }
}
