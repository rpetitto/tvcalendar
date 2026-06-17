package com.rpetitto.tvcalendar.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists OAuth tokens in [EncryptedSharedPreferences] so the access token and
 * refresh token never sit in plaintext on disk.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            // A refresh is only returned on first authorization; never overwrite
            // a stored refresh token with an empty value.
            .apply {
                if (refreshToken.isNotEmpty()) putString(KEY_REFRESH_TOKEN, refreshToken)
            }
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    /** Updates only the access token + expiry after a silent refresh. */
    fun updateAccessToken(accessToken: String, expiresAt: Long) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun hasTokens(): Boolean = getRefreshToken() != null

    /** True if a non-expired access token is present (60s safety buffer). */
    fun isTokenValid(): Boolean {
        val token = getAccessToken() ?: return false
        if (token.isEmpty()) return false
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return System.currentTimeMillis() < expiresAt - EXPIRY_BUFFER_MS
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "tvcalendar_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val EXPIRY_BUFFER_MS = 60_000L
    }
}
