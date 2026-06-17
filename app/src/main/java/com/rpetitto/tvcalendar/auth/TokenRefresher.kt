package com.rpetitto.tvcalendar.auth

import android.util.Log

/**
 * Exchanges a stored refresh token for a fresh access token. Used silently by
 * the repository whenever the cached access token is expired or missing.
 */
class TokenRefresher(
    private val api: OAuthApi = OAuthApi.create(),
) {

    /**
     * Returns a new access token (and persists it via [tokenStore] when given),
     * or null on failure. On invalid_grant the refresh token is no longer valid
     * and the caller should force re-authentication.
     */
    suspend fun refresh(
        clientId: String,
        clientSecret: String,
        refreshToken: String,
        tokenStore: TokenStore? = null,
    ): String? {
        val response = try {
            api.refreshToken(
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = refreshToken,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Token refresh network error", e)
            return null
        }

        val body = response.body()
        val accessToken = body?.accessToken
        if (response.isSuccessful && accessToken != null) {
            val expiresAt = System.currentTimeMillis() + (body.expiresIn ?: 0L) * 1000
            tokenStore?.updateAccessToken(accessToken, expiresAt)
            return accessToken
        }

        Log.w(TAG, "Token refresh failed: ${response.code()} ${response.errorBody()?.string()}")
        return null
    }

    companion object {
        private const val TAG = "TokenRefresher"
    }
}
