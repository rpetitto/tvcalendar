package com.rpetitto.tvcalendar.auth

import com.google.gson.annotations.SerializedName
import com.rpetitto.tvcalendar.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Retrofit interface for Google's OAuth 2.0 Device Flow endpoints.
 *
 * Both the device-code request and the token poll/refresh hit
 * https://oauth2.googleapis.com/ which is a different host from the Calendar
 * API, so this gets its own Retrofit instance (see [OAuthApi.create]).
 */
interface OAuthApi {

    @FormUrlEncoded
    @POST("device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String,
    ): DeviceCodeResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun pollToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = GRANT_TYPE_DEVICE,
    ): retrofit2.Response<TokenResponse>

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = GRANT_TYPE_REFRESH,
    ): retrofit2.Response<TokenResponse>

    companion object {
        const val DEVICE_CODE_SCOPE = "https://www.googleapis.com/auth/calendar.readonly"
        const val GRANT_TYPE_DEVICE = "urn:ietf:params:oauth:grant-type:device_code"
        const val GRANT_TYPE_REFRESH = "refresh_token"

        private const val BASE_URL = "https://oauth2.googleapis.com/"

        fun create(): OAuthApi {
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.HTTP_LOGGING) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OAuthApi::class.java)
        }
    }
}

/** Response from POST /device/code. */
data class DeviceCodeResponse(
    @SerializedName("device_code") val deviceCode: String,
    @SerializedName("user_code") val userCode: String,
    // Google returns "verification_url"; the spec also allows "verification_uri".
    @SerializedName("verification_url") val verificationUrl: String?,
    @SerializedName("verification_uri") val verificationUri: String?,
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("interval") val interval: Long,
) {
    /** The URL the user should visit, regardless of which field Google populated. */
    val verification: String
        get() = verificationUrl ?: verificationUri ?: "https://www.google.com/device"
}

/** Response from POST /token (poll, success, or error body). */
data class TokenResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Long?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("scope") val scope: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_description") val errorDescription: String?,
)
