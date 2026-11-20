package com.example.resq.ui.screen.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.model.EmergencyContact
import com.example.resq.ui.theme.BackgroundLight
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Screen to create a new community.
 * Design: back button + title "Create Community", community photo picker (optional),
 * community name field, Invite members row (opens SelectToGroupScreen), Create button.
 */
@Composable
fun CreateCommunityScreen(
    allContacts   : List<EmergencyContact>,
    onCreateClick : (name: String, memberIds: List<Int>) -> Unit,
    onBack        : () -> Unit,
) {
    var communityName    by remember { mutableStateOf("") }
    var nameError        by remember { mutableStateOf(false) }
    var selectedMembers  by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var showSelectScreen by remember { mutableStateOf(false) }

    if (showSelectScreen) {
        SelectToGroupScreen(
            allContacts     = allContacts,
            selectedIds     = selectedMembers.map { it.id }.toSet(),
            onConfirm       = { chosen ->
                selectedMembers  = chosen
                showSelectScreen = false
            },
            onBack          = { showSelectScreen = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint               = TextPrimary,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text       = "Create Community",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                color      = TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text       = "Bring your people together.",
            fontFamily = PoppinsFamily,
            fontSize   = 13.sp,
            color      = TextSecondary,
            modifier   = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Community photo picker ─────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BackgroundLight)
                .clickable { /* photo picker — backend/storage integration later */ }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ResQRed.copy(alpha = 0.12f))
                    .border(1.5.dp, ResQRed, CircleShape),
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Add photo",
                    tint               = ResQRed,
                    modifier           = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    text       = "Add community photo",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                )
                Text(
                    text       = "Upload a photo · optional",
                    fontFamily = PoppinsFamily,
                    fontSize   = 12.sp,
                    color      = TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Community name ─────────────────────────────────────────────────
        Text(
            text       = "Community name",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp,
            color      = TextPrimary,
            modifier   = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value         = communityName,
            onValueChange = { communityName = it; nameError = false },
            placeholder   = {
                Text("e.g. Family, Office Staff", fontFamily = PoppinsFamily, fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.6f))
            },
            singleLine    = true,
            isError       = nameError,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = ResQRed,
                unfocusedBorderColor = Color(0xFFE5E7EB),
                errorBorderColor     = ResQRed,
                cursorColor          = ResQRed,
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary,
            ),
            textStyle = LocalTextStyle.current.copy(fontFamily = PoppinsFamily, fontSize = 13.sp),
        )
        if (nameError) {
            Text(
                text     = "Nama komunitas tidak boleh kosong",
                color    = ResQRed,
                fontSize = 11.sp,
                fontFamily = PoppinsFamily,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Invite members row ─────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BackgroundLight)
                .clickable { showSelectScreen = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Invite members",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                )
                Text(
                    text       = if (selectedMembers.isEmpty())
                        "Add people now or invite them later"
                    else
                        "${selectedMembers.size} member dipilih: ${selectedMembers.joinToString(", ") { it.name }}",
                    fontFamily = PoppinsFamily,
                    fontSize   = 12.sp,
                    color      = TextSecondary,
                    lineHeight = 18.sp,
                )
            }
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "Invite",
                tint               = ResQRed,
                modifier           = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── Create button ──────────────────────────────────────────────────
        Button(
            onClick = {
                nameError = communityName.isBlank()
                if (!nameError) {
                    onCreateClick(communityName.trim(), selectedMembers.map { it.id })
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ResQRed, contentColor = Color.White),
        ) {
            Text(
                text       = "Create",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
