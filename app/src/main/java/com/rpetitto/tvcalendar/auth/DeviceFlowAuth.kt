package com.rpetitto.tvcalendar.auth

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Drives Google's OAuth 2.0 Device Flow for limited-input devices.
 *
 * 1. [requestDeviceCode] asks Google for a user code + verification URL.
 * 2. The UI shows that code; the user authorizes on their phone.
 * 3. [pollForToken] polls the token endpoint until the user finishes.
 */
class DeviceFlowAuth(
    private val api: OAuthApi = OAuthApi.create(),
) {

    /** Sealed result emitted while polling for the token. */
    sealed interface TokenResult {
        /** User has not authorized yet — keep polling. */
        data object Pending : TokenResult

        /** Authorization complete. */
        data class Success(
            val accessToken: String,
            val refreshToken: String,
            val expiresAt: Long,
        ) : TokenResult

        /** Terminal failure (denied, expired, network, etc.). */
        data class Error(val reason: String) : TokenResult
    }

    suspend fun requestDeviceCode(clientId: String): DeviceCodeResponse =
        api.requestDeviceCode(
            clientId = clientId,
            scope = OAuthApi.DEVICE_CODE_SCOPE,
        )

    /**
     * Polls the token endpoint every [interval] seconds. Emits [TokenResult.Pending]
     * on each attempt that is still waiting, then a terminal [TokenResult.Success]
     * or [TokenResult.Error]. The flow completes after a terminal result.
     */
    fun pollForToken(
        clientId: String,
        clientSecret: String,
        deviceCode: String,
        interval: Long,
    ): Flow<TokenResult> = flow {
        var pollInterval = interval.coerceAtLeast(MIN_INTERVAL_SECONDS)
        while (true) {
            delay(pollInterval * 1000)

            val response = try {
                api.pollToken(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    deviceCode = deviceCode,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Token poll network error", e)
                emit(TokenResult.Pending)
                continue
            }

            val body = response.body() ?: response.errorBody()?.let { parseError(it.string()) }
            val accessToken = body?.accessToken
            val error = body?.error

            when {
                response.isSuccessful && accessToken != null -> {
                    val expiresAt = System.currentTimeMillis() + (body.expiresIn ?: 0L) * 1000
                    emit(
                        TokenResult.Success(
                            accessToken = accessToken,
                            refreshToken = body.refreshToken.orEmpty(),
                            expiresAt = expiresAt,
                        )
                    )
                    return@flow
                }

                error == "authorization_pending" -> emit(TokenResult.Pending)

                error == "slow_down" -> {
                    pollInterval += SLOW_DOWN_STEP_SECONDS
                    emit(TokenResult.Pending)
                }

                error == "access_denied" -> {
                    emit(TokenResult.Error("Access was denied on the phone."))
                    return@flow
                }

                error == "expired_token" -> {
                    emit(TokenResult.Error("The code expired. Please restart pairing."))
                    return@flow
                }

                else -> {
                    emit(TokenResult.Error(body?.errorDescription ?: error ?: "Unknown error"))
                    return@flow
                }
            }
        }
    }

    private fun parseError(raw: String): TokenResponse? = try {
        com.google.gson.Gson().fromJson(raw, TokenResponse::class.java)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse token error body: $raw", e)
        null
    }

    companion object {
        private const val TAG = "DeviceFlowAuth"
        private const val MIN_INTERVAL_SECONDS = 5L
        private const val SLOW_DOWN_STEP_SECONDS = 5L
    }
}
