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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.theme.PoppinsFamily
import com.example.resq.ui.theme.ResQRed
import com.example.resq.ui.theme.TextPrimary
import com.example.resq.ui.theme.TextSecondary

/**
 * Login Screen - sesuai screenshot "Login":
 * - Header merah gradien dengan logo RESQ di tengah
 * - Card putih melengkung di bawah dengan form Sign In
 * - Field: Email, Password (+ Forgot Password)
 * - Tombol merah "Sign In"
 * - Divider "or"
 * - Tombol Continue with Google & Apple
 * - Link "Sign Up"
 */
@Composable
fun LoginScreen(
    onLoginClick: (email: String, password: String) -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
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
            // Logo RESQ + Pin di header merah
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

            // Judul "Sign In"
            Text(
                text = "Sign In",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )

            // ── Field Email ────────────────────────────────────────────────
            LoginFieldLabel(text = "Email")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                placeholder = {
                    Text(
                        text = "Enter email",
                        fontFamily = PoppinsFamily,
                        fontSize = 13.sp,
                        color = TextSecondary.copy(alpha = 0.6f),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

            Spacer(modifier = Modifier.height(16.dp))

            // ── Field Password + Forgot Password ───────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LoginFieldLabel(text = "Password")
                Text(
                    text = "Forgot Password?",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = ResQRed,
                    modifier = Modifier.clickable { },
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
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

            // ── Tombol Sign In ─────────────────────────────────────────────
            Button(
                onClick = {
                    when {
                        email.isBlank() -> errorMessage = "Email tidak boleh kosong"
                        password.isBlank() -> errorMessage = "Password tidak boleh kosong"
                        else -> {
                            errorMessage = null
                            onLoginClick(email.trim(), password)
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
                    text = "Sign In",
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
                // Huruf "G" merah sebagai pengganti logo Google
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

            // ── Link Sign Up ───────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Don't have an account? ",
                    fontFamily = PoppinsFamily,
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
                Text(
                    text = "Sign Up",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ResQRed,
                    modifier = Modifier.clickable { onNavigateToRegister() },
                )
            }
        }
    }
}

@Composable
private fun LoginFieldLabel(text: String) {
    Text(
        text = text,
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = TextPrimary,
        modifier = Modifier.fillMaxWidth(),
    )
}
