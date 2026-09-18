package com.onip.cartoonip.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

fun buildRetrofit(baseUrl: String, tokenProvider: () -> String?): Retrofit {
    val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
