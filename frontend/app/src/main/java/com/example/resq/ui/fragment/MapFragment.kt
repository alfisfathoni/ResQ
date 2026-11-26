package com.example.resq.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.resq.ui.navigation.NavigationHost
import com.example.resq.ui.screen.MapScreen
import com.example.resq.ui.theme.ResQTheme

/**
 * Fragment hosting the full interactive Map screen.
 * Requests ACCESS_FINE_LOCATION at runtime before showing the map.
 */
class MapFragment : Fragment() {

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* OSMDroid handles showing map regardless; location dot won't appear if denied */ }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val navHost = activity as? NavigationHost

        // Request location permission if not yet granted
        val fine   = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(requireContext(), fine)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(arrayOf(fine, coarse))
        }

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
