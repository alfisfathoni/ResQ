package com.example.resq.data

import android.content.Context
import android.content.SharedPreferences
import com.example.resq.ui.model.EmergencyContact

/**
 * Manages user session state, authentication persistence,
 * and user's saved emergency contacts.
 */
class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "resq_user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_EMERGENCY_CONTACTS = "emergency_contacts"
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

    var userPhone: String?
        get() = prefs.getString(KEY_USER_PHONE, null)
        set(value) {
            prefs.edit().putString(KEY_USER_PHONE, value).apply()
        }

    /**
     * Saves user session upon successful login or registration.
     */
    fun saveUserSession(email: String, name: String? = null, phone: String? = null) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_EMAIL, email)
            .apply {
                if (name != null) putString(KEY_USER_NAME, name)
                if (phone != null) putString(KEY_USER_PHONE, phone)
            }
            .apply()
    }

    /**
     * Retrieves saved emergency contacts entered by user.
     * Returns empty list if user has not entered contacts yet.
     */
    fun getEmergencyContacts(): List<EmergencyContact> {
        val raw = prefs.getString(KEY_EMERGENCY_CONTACTS, null) ?: return emptyList()
        return try {
            raw.split(";;;").filter { it.isNotBlank() }.mapIndexed { index, item ->
                val parts = item.split("|")
                EmergencyContact(
                    id           = index + 1,
                    name         = parts.getOrElse(0) { "Kontak" },
                    relationship = parts.getOrElse(1) { "Family" },
                    phone        = parts.getOrElse(2) { "" },
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveEmergencyContacts(contacts: List<EmergencyContact>) {
        val raw = contacts.joinToString(";;;") { "${it.name}|${it.relationship}|${it.phone}" }
        prefs.edit().putString(KEY_EMERGENCY_CONTACTS, raw).apply()
    }

    fun addEmergencyContact(contact: EmergencyContact) {
        val current = getEmergencyContacts().toMutableList()
        // If not already in list, add it
        current.add(contact)
        saveEmergencyContacts(current)
    }

    /**
     * Clears user session and contacts (used for logout).
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
