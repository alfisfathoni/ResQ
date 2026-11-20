package com.example.resq.ui.screen.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Bottom-sheet dialog to add a new emergency contact from the Companion screen.
 * Matches the "Kontak darurat" design: Name + Relationship chips + Phone + Save button.
 */
@Composable
fun AddContactSheet(
    onSave   : (name: String, relationship: String, phone: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name         by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Family") }
    var phone        by remember { mutableStateOf("") }
    var nameError    by remember { mutableStateOf(false) }
    var phoneError   by remember { mutableStateOf(false) }

    val relationships = listOf("Family", "Spouse/Partner", "Friend", "Neighbour", "etc")

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier         = Modifier
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
                    .padding(top = 20.dp, bottom = 36.dp),
            ) {
                // Close button
                Row(
                    modifier       = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint               = TextPrimary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text       = "Add emergency contact",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = TextPrimary,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text       = "Enter the number of someone who can help you in an emergency.",
                    fontFamily = PoppinsFamily,
                    fontSize   = 13.sp,
                    color      = TextSecondary,
                    lineHeight = 20.sp,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Full name ──────────────────────────────────────────────
                Text(
                    text       = "Full name contact",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it; nameError = false },
                    placeholder   = {
                        Text("Enter full name", fontFamily = PoppinsFamily, fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.6f))
                    },
                    singleLine    = true,
                    isError       = nameError,
                    modifier      = Modifier.fillMaxWidth(),
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
                    Text("Nama tidak boleh kosong", color = ResQRed, fontSize = 11.sp, fontFamily = PoppinsFamily)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Relationship chips ─────────────────────────────────────
                Text(
                    text       = "Relationship",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    relationships.forEach { rel ->
                        val isSelected = relationship == rel
                        Box(
                            modifier         = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) ResQRed else Color.White)
                                .border(1.dp, if (isSelected) ResQRed else Color(0xFFD1D5DB), RoundedCornerShape(50.dp))
                                .clickable { relationship = rel }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text       = rel,
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 12.sp,
                                color      = if (isSelected) Color.White else TextSecondary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Phone number ───────────────────────────────────────────
                Text(
                    text       = "Phone number",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value         = phone,
                    onValueChange = { phone = it; phoneError = false },
                    placeholder   = {
                        Text("Enter phone number", fontFamily = PoppinsFamily, fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.6f))
                    },
                    singleLine    = true,
                    isError       = phoneError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier      = Modifier.fillMaxWidth(),
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
                if (phoneError) {
                    Text("Nomor telepon tidak boleh kosong", color = ResQRed, fontSize = 11.sp, fontFamily = PoppinsFamily)
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        nameError  = name.isBlank()
                        phoneError = phone.isBlank()
                        if (!nameError && !phoneError) {
                            onSave(name.trim(), relationship, phone.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ResQRed, contentColor = Color.White),
                ) {
                    Text(
                        text       = "Save",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                    )
                }
            }
        }
    }
}
