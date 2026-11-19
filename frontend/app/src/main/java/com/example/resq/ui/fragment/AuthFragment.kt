package com.example.resq.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.example.resq.data.UserSessionManager
import com.example.resq.ui.model.EmergencyContact
import com.example.resq.ui.navigation.NavigationHost
import com.example.resq.ui.screen.EmergencyContactScreen
import com.example.resq.ui.screen.LoginScreen
import com.example.resq.ui.screen.RegisterScreen
import com.example.resq.ui.screen.VerificationScreen
import com.example.resq.ui.theme.ResQTheme

private enum class AuthSubScreen {
    LOGIN,
    REGISTER,
    VERIFICATION,
    EMERGENCY_CONTACT,
}

/**
 * Fragment responsible for the entire Authentication & Onboarding flow:
 * Login <-> Register -> Verification -> Emergency Contacts (1/3 -> 2/3 -> 3/3).
 */
class AuthFragment : Fragment() {

    private lateinit var sessionManager: UserSessionManager

    companion object {
        private const val ARG_START_STEP = "arg_start_step"

        fun newInstance(emergencyContactStep: Int = 0): AuthFragment {
            return AuthFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_START_STEP, emergencyContactStep)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = UserSessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val initialStep = arguments?.getInt(ARG_START_STEP, 0) ?: 0

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ResQTheme {
                    val initialScreen = if (initialStep > 0) {
                        AuthSubScreen.EMERGENCY_CONTACT
                    } else {
                        AuthSubScreen.LOGIN
                    }

                    var currentSubScreen by remember { mutableStateOf(initialScreen) }
                    var pendingEmail by remember { mutableStateOf("") }
                    var pendingName by remember { mutableStateOf("") }
                    var emergencyContactStep by remember {
                        mutableIntStateOf(if (initialStep > 0) initialStep else 1)
                    }

                    val navHost = activity as? NavigationHost

                    when (currentSubScreen) {
                        // ── 1. LOGIN ───────────────────────────────────────────
                        AuthSubScreen.LOGIN -> LoginScreen(
                            onLoginClick = { email, _ ->
                                sessionManager.saveUserSession(email = email)
                                Toast.makeText(requireContext(), "Login berhasil!", Toast.LENGTH_SHORT).show()
                                navHost?.navigateToHome()
                            },
                            onNavigateToRegister = {
                                currentSubScreen = AuthSubScreen.REGISTER
                            },
                        )

                        // ── 2. REGISTER ────────────────────────────────────────
                        AuthSubScreen.REGISTER -> RegisterScreen(
                            onRegisterClick = { name, email, _ ->
                                pendingEmail = email
                                pendingName = name
                                currentSubScreen = AuthSubScreen.VERIFICATION
                            },
                            onNavigateToLogin = {
                                currentSubScreen = AuthSubScreen.LOGIN
                            },
                        )

                        // ── 3. VERIFICATION ────────────────────────────────────
                        AuthSubScreen.VERIFICATION -> VerificationScreen(
                            onDismiss = {
                                currentSubScreen = AuthSubScreen.LOGIN
                            },
                            onResendEmail = {
                                Toast.makeText(
                                    requireContext(),
                                    "Email verifikasi dikirim ulang!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                emergencyContactStep = 1
                                currentSubScreen = AuthSubScreen.EMERGENCY_CONTACT
                            },
                        )

                        // ── 4. EMERGENCY CONTACT (1 -> 2 -> 3) ─────────────────
                        AuthSubScreen.EMERGENCY_CONTACT -> EmergencyContactScreen(
                            contactStep = emergencyContactStep,
                            onSaveAndContinue = { name, relationship, phone ->
                                sessionManager.addEmergencyContact(
                                    EmergencyContact(
                                        id           = emergencyContactStep,
                                        name         = name,
                                        relationship = relationship,
                                        phone        = phone,
                                    )
                                )
                                Toast.makeText(
                                    requireContext(),
                                    "Kontak $name ($relationship) disimpan",
                                    Toast.LENGTH_SHORT,
                                ).show()

                                if (emergencyContactStep < 3) {
                                    emergencyContactStep++
                                } else {
                                    sessionManager.saveUserSession(
                                        email = pendingEmail,
                                        name  = pendingName,
                                    )
                                    navHost?.navigateToHome()
                                }
                            },
                            onSkip = {
                                if (emergencyContactStep < 3) {
                                    emergencyContactStep++
                                } else {
                                    sessionManager.saveUserSession(
                                        email = pendingEmail,
                                        name  = pendingName,
                                    )
                                    navHost?.navigateToHome()
                                }
                            },
                            onDismiss = {
                                sessionManager.saveUserSession(
                                    email = pendingEmail,
                                    name  = pendingName,
                                )
                                navHost?.navigateToHome()
                            },
                        )
                    }
                }
            }
        }
    }
}
