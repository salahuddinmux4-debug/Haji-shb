package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object SecurityUtils {
    private const val SALT = "MujahidAccountsSalt2026Secure"

    fun hashPassword(password: String): String {
        val input = password + SALT
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val hashed = hashPassword(password)
        return hashed.equals(storedHash, ignoreCase = true)
    }
}

enum class UserRole {
    NONE,
    ADMIN,
    CUSTOMER
}

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mujahid_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ROLE = "user_role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_TOKEN = "session_token"
        private const val KEY_AUTO_LOGIN = "auto_login"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }

    fun saveAdminSession(adminId: String, name: String, username: String) {
        prefs.edit()
            .putString(KEY_ROLE, UserRole.ADMIN.name)
            .putString(KEY_USER_ID, adminId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USERNAME, username)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_AUTO_LOGIN, true)
            .putString(KEY_TOKEN, "admin_token_${System.currentTimeMillis()}")
            .apply()
    }

    fun saveCustomerSession(customerId: String, name: String, username: String) {
        prefs.edit()
            .putString(KEY_ROLE, UserRole.CUSTOMER.name)
            .putString(KEY_USER_ID, customerId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USERNAME, username)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_AUTO_LOGIN, true)
            .putString(KEY_TOKEN, "cust_token_${System.currentTimeMillis()}")
            .apply()
    }

    fun getRole(): UserRole {
        val roleStr = prefs.getString(KEY_ROLE, UserRole.NONE.name)
        return try {
            UserRole.valueOf(roleStr ?: UserRole.NONE.name)
        } catch (_: Exception) {
            UserRole.NONE
        }
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun isAutoLoginEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_LOGIN, true)

    fun isNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ROLE)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USERNAME)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_TOKEN)
            .apply()
    }
}
