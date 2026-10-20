package com.example.resq

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.resq.ui.screen.HomeScreen
import com.example.resq.ui.theme.ResQTheme

/**
 * Entry-point activity for the RESQ application.
 *
 * Sets up edge-to-edge display and delegates all UI to Jetpack Compose
 * via [setContent]. The [ResQTheme] wrapper applies Material 3 theming,
 * brand colours, and the Poppins font family across the entire screen tree.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ResQTheme {
                HomeScreen(
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
                    onNavItemSelected = { route ->
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