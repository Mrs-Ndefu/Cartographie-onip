package com.onip.cartoonip.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// onUnauthorized : appelé quand le serveur refuse le jeton envoyé (401) — session expirée
// (jeton valable 7 jours) ou compte désactivé. Pas appelé pour la connexion elle-même, qui part
// sans jeton.
fun buildRetrofit(baseUrl: String, tokenProvider: () -> String?, onUnauthorized: () -> Unit): Retrofit {
    val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = tokenProvider()
            val request = chain.request().newBuilder().apply {
                token?.let { header("Authorization", "Bearer $it") }
            }.build()
            val response = chain.proceed(request)
            if (token != null && response.code == 401) onUnauthorized()
            response
        }
        .addInterceptor(logging)
        .build()

    return Retrofit.Builder()
        .baseUrl(normalizedBaseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
