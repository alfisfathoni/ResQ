package com.example.resq.ui.navigation

/**
 * Interface implemented by MainActivity to allow Fragments
 * to request navigation to other screens/fragments cleanly.
 */
interface NavigationHost {
    fun navigateToSplash()
    fun navigateToAuth()
    fun navigateToHome()
    fun navigateToProfile()
    fun navigateToEmergencyContact(step: Int)
}
