package com.example.resq.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.example.resq.data.UserSessionManager
import com.example.resq.ui.navigation.NavigationHost
import com.example.resq.ui.screen.ProfileScreen
import com.example.resq.ui.theme.ResQTheme

/**
 * Fragment hosting the User Profile Screen with dynamic Emergency Contacts list
 * and full bottom navbar routing (active on "profile").
 */
class ProfileFragment : Fragment() {

    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = UserSessionManager(requireContext())
    }

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
                    ProfileScreen(
                        userName          = sessionManager.userName ?: "ForestGump",
                        userEmail         = sessionManager.userEmail ?: "gumpfor@gmail.com",
                        userPhone         = sessionManager.userPhone ?: "+62 89507274535",
                        emergencyContacts = sessionManager.getEmergencyContacts(),
                        onAddContactClick = {
                            val nextStep = (sessionManager.getEmergencyContacts().size + 1).coerceAtMost(3)
                            navHost?.navigateToEmergencyContact(nextStep)
                        },
                        onNavItemSelected = { route ->
                            when (route) {
                                "home"    -> navHost?.navigateToHome()
                                "map"     -> navHost?.navigateToMap()
                                "report"  -> navHost?.navigateToReport()
                                "profile" -> { /* already on profile */ }
                            }
                        },
                        onCommunityFabClick = { navHost?.navigateToCompanion() },
                        onLogout          = {
                            sessionManager.clearSession()
                            Toast.makeText(requireContext(), "Berhasil logout", Toast.LENGTH_SHORT).show()
                            navHost?.navigateToAuth()
                        },
                    )
                }
            }
        }
    }
}
