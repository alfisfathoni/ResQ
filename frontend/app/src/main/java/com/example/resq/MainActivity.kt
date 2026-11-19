package com.example.resq

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.resq.ui.fragment.AuthFragment
import com.example.resq.ui.fragment.HomeFragment
import com.example.resq.ui.fragment.ProfileFragment
import com.example.resq.ui.fragment.SplashFragment
import com.example.resq.ui.navigation.NavigationHost

/**
 * Main Activity acting purely as the Host for UI Fragments:
 * - SplashFragment
 * - AuthFragment (Login, Register, Verification, Emergency Contacts)
 * - HomeFragment (Interactive SOS button & Responders)
 * - ProfileFragment (Profile, RecyclerView Emergency Contacts list, Help, Logout)
 */
class MainActivity : AppCompatActivity(), NavigationHost {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            replaceFragment(SplashFragment(), addToBackStack = false)
        }
    }

    override fun navigateToSplash() {
        replaceFragment(SplashFragment(), addToBackStack = false)
    }

    override fun navigateToAuth() {
        replaceFragment(AuthFragment(), addToBackStack = false)
    }

    override fun navigateToHome() {
        replaceFragment(HomeFragment(), addToBackStack = false)
    }

    override fun navigateToProfile() {
        replaceFragment(ProfileFragment(), addToBackStack = false)
    }

    override fun navigateToEmergencyContact(step: Int) {
        replaceFragment(AuthFragment.newInstance(step), addToBackStack = true)
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment)
            if (addToBackStack) {
                addToBackStack(null)
            }
        }
    }
}
