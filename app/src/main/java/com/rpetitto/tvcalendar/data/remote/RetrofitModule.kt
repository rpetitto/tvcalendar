package com.rpetitto.tvcalendar.data.remote

import com.rpetitto.tvcalendar.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Builds the Calendar API Retrofit instance. */
object RetrofitModule {

    private const val BASE_URL = "https://www.googleapis.com/"

    fun provideCalendarApiService(): CalendarApiService {
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
            .create(CalendarApiService::class.java)
    }
}
