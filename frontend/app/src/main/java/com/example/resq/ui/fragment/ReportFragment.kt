package com.example.resq.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.example.resq.ui.navigation.NavigationHost
import com.example.resq.ui.screen.ReportScreen
import com.example.resq.ui.theme.ResQTheme

/**
 * Fragment hosting the Incident Reports screen with active "report" navbar tab.
 */
class ReportFragment : Fragment() {

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
                    ReportScreen(
                        onNavItemSelected = { route ->
                            when (route) {
                                "home"    -> navHost?.navigateToHome()
                                "map"     -> navHost?.navigateToMap()
                                "report"  -> { /* already on report */ }
                                "profile" -> navHost?.navigateToProfile()
                            }
                        },
                    )
                }
            }
        }
    }
}
