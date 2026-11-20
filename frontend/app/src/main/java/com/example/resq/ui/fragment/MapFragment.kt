package com.example.resq.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.example.resq.ui.navigation.NavigationHost
import com.example.resq.ui.screen.MapScreen
import com.example.resq.ui.theme.ResQTheme

/**
 * Fragment hosting the Map screen with active "map" navbar tab.
 */
class MapFragment : Fragment() {

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
                    MapScreen(
                        onNavItemSelected = { route ->
                            when (route) {
                                "home"    -> navHost?.navigateToHome()
                                "map"     -> { /* already on map */ }
                                "report"  -> navHost?.navigateToReport()
                                "profile" -> navHost?.navigateToProfile()
                            }
                        },
                        onCommunityFabClick = { navHost?.navigateToCompanion() },
                    )
                }
            }
        }
    }
}
