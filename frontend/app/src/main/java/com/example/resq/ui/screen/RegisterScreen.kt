package com.example.resq.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Register Screen - sesuai screenshot "Register":
 * - Header merah gradien dengan logo RESQ di tengah
 * - Card putih melengkung di bawah dengan form Create Account
 * - Field: Full name, Username, Email, Phone number, Password
 * - Catatan "Password must contain at least 8 characters."
 * - Tombol merah "Sign Up"
 * - Divider "or"
 * - Tombol Continue with Google & Apple
 * - Link "Sign In"
 */
@Composable
fun RegisterScreen(
    onRegisterClick: (name: String, email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Header Merah Gradien ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFEB2A2E), Color(0xFFD01B20))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "RESQ",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PoppinsFamily,
                    letterSpacing = (-0.5).sp,
                )
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "Pin",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 4.dp),
                )
            }
        }

        // ── Card Putih melengkung di atas header merah ─────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 180.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Judul "Create Account"
            Text(
                text = "Create Account",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )

            // ── Full name ──────────────────────────────────────────────────
            RegFieldLabel("Fullname")
            Spacer(modifier = Modifier.height(6.dp))
            RegTextField(
                value = fullName,
                onValueChange = { fullName = it; errorMessage = null },
                placeholder = "Enter full name",
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Username ───────────────────────────────────────────────────
            RegFieldLabel("Username")
            Spacer(modifier = Modifier.height(6.dp))
            RegTextField(
                value = username,
                onValueChange = { username = it; errorMessage = null },
                placeholder = "Enter username",
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Email ──────────────────────────────────────────────────────
            RegFieldLabel("Email")
            Spacer(modifier = Modifier.height(6.dp))
            RegTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                placeholder = "Enter email",
                keyboardType = KeyboardType.Email,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Phone number ───────────────────────────────────────────────
            RegFieldLabel("Phone number")
            Spacer(modifier = Modifier.height(6.dp))
            RegTextField(
                value = phone,
                onValueChange = { phone = it; errorMessage = null },
                placeholder = "Enter phone number",
                keyboardType = KeyboardType.Phone,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Password ───────────────────────────────────────────────────
            RegFieldLabel("Password")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                placeholder = {
                    Text(
                        text = "Enter password",
                        fontFamily = PoppinsFamily,
                        fontSize = 13.sp,
                        color = TextSecondary.copy(alpha = 0.6f),
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

            // Catatan password
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Password must contain at least 8 characters.",
                fontFamily = PoppinsFamily,
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )

            // Error message
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

            Spacer(modifier = Modifier.height(24.dp))

            // ── Tombol Sign Up ─────────────────────────────────────────────
            Button(
                onClick = {
                    when {
                        fullName.isBlank() -> errorMessage = "Full name is required"
                        username.isBlank() -> errorMessage = "Username is required"
                        email.isBlank() -> errorMessage = "Email is required"
                        phone.isBlank() -> errorMessage = "Phone number is required"
                        password.isBlank() -> errorMessage = "Password is required"
                        password.length < 8 -> errorMessage = "Password must contain at least 8 characters."
                        else -> {
                            errorMessage = null
                            onRegisterClick(fullName.trim(), email.trim(), password)
                        }
                    }
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
                    text = "Sign Up",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Divider "or" ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                Text(
                    text = "  or  ",
                    fontFamily = PoppinsFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Continue with Google ───────────────────────────────────────
            OutlinedButton(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = TextPrimary,
                ),
            ) {
                Text(
                    text = "G",
                    color = Color(0xFFEA4335),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = PoppinsFamily,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue with Google",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Continue with Apple ────────────────────────────────────────
            OutlinedButton(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = TextPrimary,
                ),
            ) {
                Text(
                    text = "",
                    color = Color(0xFF1C1C1E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue with Apple",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Link Sign In ───────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Already have an account? ",
                    fontFamily = PoppinsFamily,
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
                Text(
                    text = "Sign In",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ResQRed,
                    modifier = Modifier.clickable { onNavigateToLogin() },
                )
            }
        }
    }
}

@Composable
private fun RegFieldLabel(text: String) {
    Text(
        text = text,
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = TextPrimary,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RegTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                fontFamily = PoppinsFamily,
                fontSize = 13.sp,
                color = TextSecondary.copy(alpha = 0.6f),
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
}
