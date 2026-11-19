package com.example.resq.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.example.resq.data.UserSessionManager
import com.example.resq.ui.navigation.NavigationHost
import com.example.resq.ui.screen.SplashScreen
import com.example.resq.ui.theme.ResQTheme

/**
 * Fragment hosting the initial Splash Screen.
 * After countdown completes, redirects user to Home (if logged in) or Auth (if not).
 */
class SplashFragment : Fragment() {

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
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ResQTheme {
                    SplashScreen(
                        durationMillis = 2000L,
                        onSplashFinished = {
                            val host = activity as? NavigationHost
                            if (sessionManager.isLoggedIn) {
                                host?.navigateToHome()
                            } else {
                                host?.navigateToAuth()
                            }
                        },
                    )
                }
            }
        }
    }
}
