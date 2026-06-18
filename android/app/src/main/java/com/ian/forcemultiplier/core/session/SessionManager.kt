package com.ian.forcemultiplier.core.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fm_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return getAuthToken() != null
    }
}
