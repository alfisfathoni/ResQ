package com.example.resq.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.example.resq.ui.navigation.NavigationHost
import com.example.resq.ui.screen.HomeScreen
import com.example.resq.ui.theme.ResQTheme

/**
 * Fragment hosting the full interactive Home & SOS Screen.
 */
class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val navHost = activity as? NavigationHost

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ResQTheme {
                    HomeScreen(
                        onSosActivated = {
                            Toast.makeText(
                                requireContext(),
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
                                    requireContext(),
                                    "Tidak dapat membuka dialer: ${e.message}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        onResponderClick = { responder ->
                            Toast.makeText(
                                requireContext(),
                                "Calling ${responder.label}…",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        onNavItemSelected = { route ->
                            when (route) {
                                "home"    -> { /* already on home */ }
                                "map"     -> navHost?.navigateToMap()
                                "report"  -> navHost?.navigateToReport()
                                "profile" -> navHost?.navigateToProfile()
                            }
                        },
                        onCommunityFabClick = {
                            Toast.makeText(
                                requireContext(),
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
