package com.example.resq.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.theme.BackgroundLight
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Login screen for the ResQ application.
 *
 * Full-featured premium design matching the ResQ brand:
 * - Emergency status banner at the top
 * - ResQ logo with shield icon and tagline
 * - Welcome headline + subtitle
 * - Email/phone & password inputs with icons
 * - Remember me toggle + forgot password link
 * - Primary login CTA button
 * - Face ID / Fingerprint biometric button
 * - Social login (Google, Apple)
 * - Quick SOS emergency strip
 * - Register navigation link
 * - Security footer note
 *
 * @param onLoginClick Called with (email, password) when the user taps "Masuk ke ResQ".
 * @param onNavigateToRegister Called when the user taps "Daftar Sekarang".
 */
@Composable
fun LoginScreen(
    onLoginClick: (email: String, password: String) -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── 1. Emergency Status Banner ───────────────────────────────────────
            EmergencyStatusBanner()

            Spacer(modifier = Modifier.height(24.dp))

            // ── 2. ResQ Logo + Tagline ───────────────────────────────────────────
            ResQLoginLogo()

            Spacer(modifier = Modifier.height(20.dp))

            // ── 3. Welcome Heading ───────────────────────────────────────────────
            WelcomeHeading()

            Spacer(modifier = Modifier.height(6.dp))

            HorizontalDivider(
                color = Color(0xFFE5E7EB),
                thickness = 1.dp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 4. Email / Phone Field ───────────────────────────────────────────
            LoginInputField(
                label = "Email atau Nomor Telepon",
                isRequired = true,
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                placeholder = "nama@email.com atau +628...",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── 5. Password Field ────────────────────────────────────────────────
            PasswordInputField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                passwordVisible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── 6. Remember Me + Forgot Password ─────────────────────────────────
            RememberMeRow(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
            )

            // ── 7. Error Message ─────────────────────────────────────────────────
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = ResQRed,
                    fontFamily = PoppinsFamily,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 8. Primary Login Button ───────────────────────────────────────────
            Button(
                onClick = {
                    when {
                        email.isBlank() -> errorMessage = "Email atau nomor telepon tidak boleh kosong"
                        password.isBlank() -> errorMessage = "Kata sandi tidak boleh kosong"
                        else -> {
                            errorMessage = null
                            onLoginClick(email.trim(), password)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ResQRed,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                ),
            ) {
                Text(
                    text = "Masuk ke ResQ  →",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.3.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 9. Biometric Button ───────────────────────────────────────────────
            OutlinedButton(
                onClick = { /* TODO: biometric auth */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint",
                    tint = ResQRed,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gunakan Face ID / Fingerprint",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 10. Social Login Divider ──────────────────────────────────────────
            SocialDivider()

            Spacer(modifier = Modifier.height(14.dp))

            // ── 11. Social Buttons Row ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SocialLoginButton(
                    label = "Google",
                    icon = Icons.Default.Email,
                    iconTint = Color(0xFFEA4335),
                    modifier = Modifier.weight(1f),
                )
                SocialLoginButton(
                    label = "Apple",
                    icon = Icons.Default.Lock,
                    iconTint = Color(0xFF1C1C1E),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 12. SOS Emergency Strip ───────────────────────────────────────────
            SosEmergencyStrip()

            Spacer(modifier = Modifier.height(20.dp))

            // ── 13. Register Link ─────────────────────────────────────────────────
            RegisterLink(onNavigateToRegister = onNavigateToRegister)

            Spacer(modifier = Modifier.height(16.dp))

            // ── 14. Security Footer ───────────────────────────────────────────────
            SecurityFooter()
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmergencyStatusBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.5.dp,
                color = ResQRed.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
            )
            .background(Color(0xFFFFF5F5))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(ResQRed, CircleShape),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SIAGA DARURAT 24/7 AKTIF",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = ResQRed,
                letterSpacing = 0.5.sp,
            )
        }
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "Shield",
            tint = ResQRed,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ResQLoginLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    brush = Brush.radialGradient(listOf(Color(0xFFFF6B6B), ResQRed)),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "ResQ Logo",
                tint = Color.White,
                modifier = Modifier.size(38.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = TextPrimary,
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        letterSpacing = (-0.5).sp,
                    )
                ) { append("Res") }
                withStyle(
                    SpanStyle(
                        color = ResQRed,
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        letterSpacing = (-0.5).sp,
                    )
                ) { append("Q") }
            },
        )

        Text(
            text = "All-in-One Emergency & Safety Lifeline",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = ResQRed,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WelcomeHeading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Selamat Datang Kembali",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Masuk untuk mengakses layanan pemantauan\ndarurat & respon cepat real-time.",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun LoginInputField(
    label: String,
    isRequired: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row {
            Text(
                text = label,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary,
            )
            if (isRequired) {
                Text(
                    text = " *",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = ResQRed,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = PoppinsFamily,
                    fontSize = 13.sp,
                    color = TextSecondary.copy(alpha = 0.7f),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = label,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
    }
}

@Composable
private fun PasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row {
            Text(
                text = "Kata Sandi",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary,
            )
            Text(
                text = " *",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = ResQRed,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = "••••••••",
                    fontFamily = PoppinsFamily,
                    fontSize = 13.sp,
                    color = TextSecondary.copy(alpha = 0.7f),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            },
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Sembunyikan" else "Tampilkan",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
    }
}

@Composable
private fun RememberMeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onCheckedChange(!checked) },
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = ResQRed,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = Color.White,
                ),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Ingat Saya",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = TextPrimary,
            )
        }
        Text(
            text = "Lupa Kata Sandi?",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = ResQRed,
            modifier = Modifier.clickable { /* TODO: forgot password */ },
        )
    }
}

@Composable
private fun SocialDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFE5E7EB),
            thickness = 1.dp,
        )
        Text(
            text = "  Atau lanjutkan dengan  ",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = TextSecondary,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFE5E7EB),
            thickness = 1.dp,
        )
    }
}

@Composable
private fun SocialLoginButton(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = { /* TODO: social login */ },
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, Color(0xFFE5E7EB)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = TextPrimary,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = TextPrimary,
        )
    }
}

@Composable
private fun SosEmergencyStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.5.dp,
                color = ResQRed.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp),
            )
            .background(Color(0xFFFFF5F5))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ResQRed, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "SOS",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Kondisi Darurat?",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                )
                Text(
                    text = "Gunakan tombol SOS tanpa masuk",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = TextSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Button(
            onClick = { /* TODO: SOS without login */ },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ResQRed,
                contentColor = Color.White,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        ) {
            Text(
                text = "SOS\nCepat",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun RegisterLink(onNavigateToRegister: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Belum memiliki akun ResQ? ",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = TextSecondary,
        )
        Text(
            text = "Daftar Sekarang",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = ResQRed,
            textDecoration = TextDecoration.None,
            modifier = Modifier.clickable { onNavigateToRegister() },
        )
    }
}

@Composable
private fun SecurityFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Secure",
            tint = TextSecondary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Terhubung aman dengan enkripsi data medis & lokasi darurat",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
        )
    }
}
