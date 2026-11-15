package com.example.resq.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user session state and authentication persistence.
 * Uses Android's SharedPreferences to remember whether the user
 * has previously logged in or registered.
 */
class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "resq_user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
    }

    /**
     * Returns true if user has already logged in or registered.
     */
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()
        }

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) {
            prefs.edit().putString(KEY_USER_EMAIL, value).apply()
        }

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) {
            prefs.edit().putString(KEY_USER_NAME, value).apply()
        }

    /**
     * Saves user session upon successful login or registration.
     */
    fun saveUserSession(email: String, name: String? = null) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_EMAIL, email)
            .apply {
                if (name != null) {
                    putString(KEY_USER_NAME, name)
                }
            }
            .apply()
    }

    /**
     * Clears user session (used for logout).
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
