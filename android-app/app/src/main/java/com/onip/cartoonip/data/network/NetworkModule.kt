package com.onip.cartoonip.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a Retrofit instance pointed at [baseUrl]. [tokenProvider] is read on every request so a
 * token obtained after this client was built (or a token that changes on re-login) is always
 * picked up — callers should not need to rebuild the client just because the session refreshed.
 */
fun buildRetrofit(baseUrl: String, tokenProvider: () -> String?): Retrofit {
    val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder().apply {
                tokenProvider()?.let { header("Authorization", "Bearer $it") }
            }.build()
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .build()

    return Retrofit.Builder()
        .baseUrl(normalizedBaseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
