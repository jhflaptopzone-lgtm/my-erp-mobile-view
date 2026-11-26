package com.example.data.local

import android.content.Context
import com.example.data.model.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "jhf_erp_session",
        Context.MODE_PRIVATE
    )
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val DEFAULT_SERVER_URL = "https://jhflaptop.cloud.run"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER = "user_profile"
    }

    fun getServerUrl(): String {
        val raw = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        return normalizeUrl(raw)
    }

    fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, normalizeUrl(url)).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun getUser(): UserProfile? {
        val raw = prefs.getString(KEY_USER, null) ?: return null
        return runCatching { json.decodeFromString<UserProfile>(raw) }.getOrNull()
    }

    fun saveSession(token: String, user: UserProfile) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, json.encodeToString(user))
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER)
            .apply()
    }

    fun normalizeUrl(input: String): String {
        var trimmed = input.trim().trimEnd('/')
        if (trimmed.isBlank()) return DEFAULT_SERVER_URL
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }
}
