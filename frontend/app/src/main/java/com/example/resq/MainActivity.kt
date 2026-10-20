package com.example.resq

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.resq.ui.screen.HomeScreen
import com.example.resq.ui.screen.LoginScreen
import com.example.resq.ui.screen.RegisterScreen
import com.example.resq.ui.theme.ResQTheme

/** Simple navigation state used before a NavController is wired up. */
private enum class Screen { LOGIN, REGISTER, HOME }

/**
 * Entry-point activity for the RESQ application.
 *
 * Sets up edge-to-edge display and delegates all UI to Jetpack Compose
 * via [setContent]. The [ResQTheme] wrapper applies Material 3 theming,
 * brand colours, and the Poppins font family across the entire screen tree.
 *
 * Navigation flow: LOGIN ↔ REGISTER → HOME
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ResQTheme {
                var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

                when (currentScreen) {
                    Screen.LOGIN -> LoginScreen(
                        onLoginClick = { email, _ ->
                            // TODO: replace with real auth logic
                            Toast.makeText(this, "Login: $email", Toast.LENGTH_SHORT).show()
                            currentScreen = Screen.HOME
                        },
                        onNavigateToRegister = { currentScreen = Screen.REGISTER },
                    )

                    Screen.REGISTER -> RegisterScreen(
                        onRegisterClick = { name, email, _ ->
                            // TODO: replace with real auth logic
                            Toast.makeText(this, "Daftar: $name ($email)", Toast.LENGTH_SHORT).show()
                            currentScreen = Screen.LOGIN
                        },
                        onNavigateToLogin = { currentScreen = Screen.LOGIN },
                    )

                    Screen.HOME -> HomeScreen(
                        onSosActivated = {
                            Toast.makeText(
                                this,
                                "🚨 SOS Activated! Help is on the way.",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                        onResponderClick = { responder ->
                            Toast.makeText(
                                this,
                                "Calling ${responder.label}…",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        onNavItemSelected = { _ ->
                            // TODO: integrate NavController for multi-screen navigation
                        },
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