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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Register screen for the ResQ application.
 *
 * Premium design matching the ResQ brand — Step 1 of 2 (Basic & Medical Info):
 * - Dark top bar with back arrow, ResQ logo, and user icon
 * - Shield pin logo + "Buat Akun ResQ" heading
 * - Step progress banner
 * - Full name, WhatsApp number, email, password fields
 * - Password strength indicator
 * - Blood type selector (A / B / AB / O)
 * - Allergy history toggle
 * - Terms & conditions checkbox
 * - Primary CTA "Daftar & Aktifkan Perlindungan"
 * - Social login (Google, Apple)
 * - Login link at the bottom
 *
 * @param onRegisterClick Called with (name, email, password) when validation passes.
 * @param onNavigateToLogin Called when the user wants to go back to the Login screen.
 */
@Composable
fun RegisterScreen(
    onRegisterClick: (name: String, email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedBloodType by remember { mutableStateOf("A") }
    var includeAllergyHistory by remember { mutableStateOf(true) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Password strength: 0=empty, 1=weak, 2=medium, 3=strong
    val passwordStrength = when {
        password.isEmpty() -> 0
        password.length < 6 -> 1
        password.length < 10 || !password.any { it.isDigit() } -> 2
        else -> 3
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
    ) {
        // ── Top Bar ─────────────────────────────────────────────────────────────
        RegisterTopBar(onNavigateToLogin = onNavigateToLogin)

        // ── Scrollable Content ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── 1. Logo ──────────────────────────────────────────────────────────
            RegisterLogo()

            Spacer(modifier = Modifier.height(16.dp))

            // ── 2. Heading ───────────────────────────────────────────────────────
            Text(
                text = "Buat Akun ResQ",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Lengkapi data untuk perlindungan darurat\n& respon lokasi real-time",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 3. Step Banner ───────────────────────────────────────────────────
            StepBanner()

            Spacer(modifier = Modifier.height(20.dp))

            // ── 4. Nama Lengkap ──────────────────────────────────────────────────
            RegisterInputField(
                label = "Nama Lengkap",
                isRequired = true,
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                placeholder = "cth. Jenifer Pilman",
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Person",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── 5. Nomor WhatsApp / Handphone ────────────────────────────────────
            PhoneInputField(
                value = phone,
                onValueChange = {
                    phone = it
                    errorMessage = null
                },
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── 6. Alamat Email ──────────────────────────────────────────────────
            RegisterInputField(
                label = "Alamat Email",
                isRequired = true,
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                placeholder = "email@anda.com",
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                },
                keyboardType = KeyboardType.Email,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── 7. Kata Sandi ────────────────────────────────────────────────────
            PasswordRegisterField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                passwordVisible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
                passwordStrength = passwordStrength,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 8. Blood Type Selector ───────────────────────────────────────────
            BloodTypeSection(
                selected = selectedBloodType,
                onSelect = { selectedBloodType = it },
                includeAllergyHistory = includeAllergyHistory,
                onAllergyToggle = { includeAllergyHistory = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 9. Error Message ─────────────────────────────────────────────────
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = ResQRed,
                    fontFamily = PoppinsFamily,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── 10. Terms Checkbox ───────────────────────────────────────────────
            TermsCheckbox(
                checked = agreedToTerms,
                onCheckedChange = { agreedToTerms = it },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 11. Primary Register Button ───────────────────────────────────────
            Button(
                onClick = {
                    when {
                        name.isBlank() -> errorMessage = "Nama lengkap tidak boleh kosong"
                        phone.isBlank() -> errorMessage = "Nomor WhatsApp tidak boleh kosong"
                        email.isBlank() -> errorMessage = "Alamat email tidak boleh kosong"
                        password.isBlank() -> errorMessage = "Kata sandi tidak boleh kosong"
                        password.length < 8 -> errorMessage = "Kata sandi minimal 8 karakter"
                        !agreedToTerms -> errorMessage = "Anda harus menyetujui Ketentuan Layanan"
                        else -> {
                            errorMessage = null
                            onRegisterClick(name.trim(), email.trim(), password)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ResQRed,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Daftar & Aktifkan\nPerlindungan",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 12. Social Login Divider ──────────────────────────────────────────
            SocialDividerReg()

            Spacer(modifier = Modifier.height(14.dp))

            // ── 13. Social Buttons ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SocialRegButton(
                    label = "Google",
                    iconTint = Color(0xFFEA4335),
                    modifier = Modifier.weight(1f),
                )
                SocialRegButton(
                    label = "Apple",
                    iconTint = Color(0xFF1C1C1E),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 14. Login Link ────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sudah punya akun ResQ? ",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
                Text(
                    text = "Masuk di sini",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ResQRed,
                    textDecoration = TextDecoration.None,
                    modifier = Modifier.clickable { onNavigateToLogin() },
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun RegisterTopBar(onNavigateToLogin: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Back arrow
        IconButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                tint = Color.White,
            )
        }

        // ResQ brand name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = ResQRed,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ResQ",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White,
            )
        }

        // Register label + user icon
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Register",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF2D2D4E), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun RegisterLogo() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFFFFE5E5), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(ResQRed, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "ResQ",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun StepBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, ResQRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF5F5))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(ResQRed, CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "LANGKAH 1 DARI 2: INFORMASI DASAR & MEDIS",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = ResQRed,
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
private fun RegisterInputField(
    label: String,
    isRequired: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingContent: @Composable () -> Unit,
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
            leadingIcon = { leadingContent() },
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
private fun PhoneInputField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Nomor WhatsApp / Handphone",
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
            Spacer(modifier = Modifier.width(6.dp))
            // SMS Darurat badge
            Box(
                modifier = Modifier
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "SMS Darurat",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = Color(0xFF2E7D32),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = "812 3456 7890",
                    fontFamily = PoppinsFamily,
                    fontSize = 13.sp,
                    color = TextSecondary.copy(alpha = 0.7f),
                )
            },
            leadingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    // +62 chip
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "+62",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = TextPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
private fun PasswordRegisterField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    passwordStrength: Int,
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
                    text = "Min. 8 karakter",
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

        // Strength bar
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            PasswordStrengthBar(strength = passwordStrength)
        }
    }
}

@Composable
private fun PasswordStrengthBar(strength: Int) {
    val strengthColor = when (strength) {
        1 -> Color(0xFFEF4444)  // weak – red
        2 -> Color(0xFFF59E0B)  // medium – amber
        3 -> Color(0xFF22C55E)  // strong – green
        else -> Color.Transparent
    }
    val strengthLabel = when (strength) {
        1 -> "Kekuatan: Lemah"
        2 -> "Kekuatan: Sedang"
        3 -> "Kekuatan: Kuat"
        else -> ""
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index < strength) strengthColor else Color(0xFFE5E7EB)
                        ),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = strengthLabel,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = strengthColor,
            )
            Text(
                text = "Kombinasikan angka & simbol",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun BloodTypeSection(
    selected: String,
    onSelect: (String) -> Unit,
    includeAllergyHistory: Boolean,
    onAllergyToggle: (Boolean) -> Unit,
) {
    val bloodTypes = listOf("A", "B", "AB", "O")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        // Header row
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFFFE5E5), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ResQRed,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Golongan Darah\nDarurat",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Krusial bagi\nAmbulan",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.End,
                lineHeight = 16.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Blood type chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            bloodTypes.forEach { type ->
                val isSelected = selected == type
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) ResQRed else Color(0xFFF3F4F6))
                        .clickable { onSelect(type) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = type,
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSelected) Color.White else TextPrimary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(color = Color(0xFFF0F0F0))

        Spacer(modifier = Modifier.height(10.dp))

        // Allergy toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Sertakan riwayat alergi / catatan medis\nsaat SOS",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 17.sp,
            )
            Switch(
                checked = includeAllergyHistory,
                onCheckedChange = onAllergyToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ResQRed,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFD1D5DB),
                ),
            )
        }
    }
}

@Composable
private fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFF5F5))
            .border(1.dp, ResQRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(10.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.Top,
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = TextPrimary,
                    )
                ) { append("Saya menyetujui ") }
                withStyle(
                    SpanStyle(
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ResQRed,
                        textDecoration = TextDecoration.Underline,
                    )
                ) { append("Ketentuan Layanan") }
                withStyle(
                    SpanStyle(
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = TextPrimary,
                    )
                ) { append(" & ") }
                withStyle(
                    SpanStyle(
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ResQRed,
                        textDecoration = TextDecoration.Underline,
                    )
                ) { append("Kebijakan Privasi") }
                withStyle(
                    SpanStyle(
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = TextPrimary,
                    )
                ) { append(" ResQ, termasuk izin transmisi lokasi darurat saat tombol SOS diaktifkan.") }
            },
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun SocialDividerReg() {
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
            text = "  Atau daftar lebih cepat dengan  ",
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
private fun SocialRegButton(
    label: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = { /* TODO: social register */ },
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, Color(0xFFE5E7EB)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = TextPrimary,
        ),
    ) {
        Icon(
            imageVector = Icons.Default.Email,
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

