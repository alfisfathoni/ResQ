package com.example.resq.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Emergency Contact Screen - sesuai screenshot "Kontak darurat":
 * - Dialog/bottom sheet putih
 * - Tombol X untuk tutup
 * - Judul "Add emergency contact"
 * - Subjudul kecil
 * - Field: Full name contact
 * - Selector Relationship: Family, Spouse/Partner, Friend, Neighbour, etc
 * - Field: Phone number
 * - Tombol merah "Save & Continue (n/3 contact)"
 * - Tombol abu-abu "Skip"
 * - Catatan opsional di bawah
 *
 * Parameter [contactStep] = 1, 2, atau 3
 */
@Composable
fun EmergencyContactScreen(
    contactStep: Int = 1,
    onSaveAndContinue: (name: String, relationship: String, phone: String) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    var contactName by remember { mutableStateOf("") }
    var selectedRelationship by remember { mutableStateOf("Family") }
    var phoneNumber by remember { mutableStateOf("") }

    val relationships = listOf("Family", "Spouse/Partner", "Friend", "Neighbour", "etc")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 40.dp),
            ) {
                // Tombol X tutup
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Text(
                        text = "✕",
                        fontSize = 16.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Judul
                Text(
                    text = "Add emergency contact",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subjudul
                Text(
                    text = "Enter the number of someone who can help you in an emergency.",
                    fontFamily = PoppinsFamily,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Full name contact ──────────────────────────────────────
                Text(
                    text = "Full name contact",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    placeholder = {
                        Text(
                            text = "Enter full name",
                            fontFamily = PoppinsFamily,
                            fontSize = 13.sp,
                            color = TextSecondary.copy(alpha = 0.6f),
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ResQRed,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = ResQRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = PoppinsFamily,
                        fontSize = 13.sp,
                    ),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Relationship chips ─────────────────────────────────────
                Text(
                    text = "Relationship",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    relationships.forEach { rel ->
                        val isSelected = selectedRelationship == rel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) ResQRed else Color.White)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ResQRed else Color(0xFFD1D5DB),
                                    shape = RoundedCornerShape(50.dp),
                                )
                                .clickable { selectedRelationship = rel }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = rel,
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else TextSecondary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Phone number ───────────────────────────────────────────
                Text(
                    text = "Phone number",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    placeholder = {
                        Text(
                            text = "Enter phone number",
                            fontFamily = PoppinsFamily,
                            fontSize = 13.sp,
                            color = TextSecondary.copy(alpha = 0.6f),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ResQRed,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = ResQRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = PoppinsFamily,
                        fontSize = 13.sp,
                    ),
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Tombol Save & Continue ─────────────────────────────────
                Button(
                    onClick = {
                        onSaveAndContinue(contactName, selectedRelationship, phoneNumber)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResQRed,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "Save & Continue ($contactStep/3 contact)",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Tombol Skip ────────────────────────────────────────────
                Button(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE5E7EB),
                        contentColor = TextPrimary,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text(
                        text = "Skip",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Catatan opsional
                Text(
                    text = "Optional, but recommended: someone to notify in an emergency.",
                    fontFamily = PoppinsFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
