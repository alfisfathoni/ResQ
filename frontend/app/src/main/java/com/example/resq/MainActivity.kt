package com.example.resq

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.resq.data.UserSessionManager
import com.example.resq.ui.screen.EmergencyContactScreen
import com.example.resq.ui.screen.HomeScreen
import com.example.resq.ui.screen.LoginScreen
import com.example.resq.ui.screen.RegisterScreen
import com.example.resq.ui.screen.SplashScreen
import com.example.resq.ui.screen.VerificationScreen
import com.example.resq.ui.theme.ResQTheme

/**
 * Alur Navigasi Lengkap sesuai Semua Screenshot:
 *
 * SPLASH (Depan - layar merah)
 *   ├─ Sudah login → HOME (Landing Page)
 *   └─ Belum login → LOGIN
 *                       ├─ Login sukses → HOME
 *                       └─ Tap Sign Up → REGISTER
 *                                           └─ Register sukses → VERIFICATION (email sent dialog)
 *                                                                   └─ Lanjut → EMERGENCY_CONTACT (step 1→2→3)
 *                                                                                   └─ Selesai/Skip → HOME
 */
private enum class Screen {
    SPLASH,
    LOGIN,
    REGISTER,
    VERIFICATION,
    EMERGENCY_CONTACT,
    HOME,
}

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = UserSessionManager(applicationContext)

        setContent {
            ResQTheme {
                var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
                // Data registrasi sementara untuk diteruskan antar layar
                var pendingEmail by remember { mutableStateOf("") }
                var pendingName by remember { mutableStateOf("") }
                // Step kontak darurat (1, 2, 3)
                var emergencyContactStep by remember { mutableStateOf(1) }

                when (currentScreen) {

                    // ── 1. SPLASH ─────────────────────────────────────────────────
                    Screen.SPLASH -> SplashScreen(
                        durationMillis = 2000L,
                        onSplashFinished = {
                            currentScreen = if (sessionManager.isLoggedIn) {
                                Screen.HOME
                            } else {
                                Screen.LOGIN
                            }
                        },
                    )

                    // ── 2. LOGIN ──────────────────────────────────────────────────
                    Screen.LOGIN -> LoginScreen(
                        onLoginClick = { email, _ ->
                            sessionManager.saveUserSession(email = email)
                            Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                            currentScreen = Screen.HOME
                        },
                        onNavigateToRegister = { currentScreen = Screen.REGISTER },
                    )

                    // ── 3. REGISTER ───────────────────────────────────────────────
                    Screen.REGISTER -> RegisterScreen(
                        onRegisterClick = { name, email, _ ->
                            pendingEmail = email
                            pendingName = name
                            currentScreen = Screen.VERIFICATION
                        },
                        onNavigateToLogin = { currentScreen = Screen.LOGIN },
                    )

                    // ── 4. VERIFICATION ───────────────────────────────────────────
                    Screen.VERIFICATION -> VerificationScreen(
                        onDismiss = { currentScreen = Screen.LOGIN },
                        onResendEmail = {
                            Toast.makeText(this, "Email verifikasi dikirim ulang!", Toast.LENGTH_SHORT).show()
                            // Setelah verifikasi, lanjut ke Emergency Contact
                            emergencyContactStep = 1
                            currentScreen = Screen.EMERGENCY_CONTACT
                        },
                    )

                    // ── 5. EMERGENCY CONTACT (step 1 → 2 → 3) ────────────────────
                    Screen.EMERGENCY_CONTACT -> EmergencyContactScreen(
                        contactStep = emergencyContactStep,
                        onSaveAndContinue = { name, relationship, phone ->
                            Toast.makeText(
                                this,
                                "Kontak $name ($relationship) disimpan",
                                Toast.LENGTH_SHORT,
                            ).show()
                            if (emergencyContactStep < 3) {
                                emergencyContactStep++
                            } else {
                                // Selesai semua kontak → simpan sesi & masuk ke Home
                                sessionManager.saveUserSession(
                                    email = pendingEmail,
                                    name = pendingName,
                                )
                                currentScreen = Screen.HOME
                            }
                        },
                        onSkip = {
                            if (emergencyContactStep < 3) {
                                emergencyContactStep++
                            } else {
                                sessionManager.saveUserSession(
                                    email = pendingEmail,
                                    name = pendingName,
                                )
                                currentScreen = Screen.HOME
                            }
                        },
                        onDismiss = {
                            // Jika ditutup dari tengah, langsung ke Home juga
                            sessionManager.saveUserSession(
                                email = pendingEmail,
                                name = pendingName,
                            )
                            currentScreen = Screen.HOME
                        },
                    )

                    // ── 6. HOME (Landing Page) ────────────────────────────────────
                    Screen.HOME -> HomeScreen(
                        onSosActivated = {
                            Toast.makeText(
                                this,
                                "🚨 Demo: Menghubungi Nomor Darurat 112...",
                                Toast.LENGTH_LONG,
                            ).show()
                            try {
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:112")
                                }
                                startActivity(dialIntent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this,
                                    "Tidak dapat membuka dialer: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onResponderClick = { responder ->
                            Toast.makeText(
                                this,
                                "Calling ${responder.label}…",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        onNavItemSelected = { },
                        onCommunityFabClick = {
                            Toast.makeText(
                                this,
                                "Opening Community…",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
            }
        }
    }
}
