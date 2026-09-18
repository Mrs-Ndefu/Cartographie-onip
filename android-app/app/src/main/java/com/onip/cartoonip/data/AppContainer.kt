package com.onip.cartoonip.data

import android.content.Context
import com.onip.cartoonip.data.network.ApiServices
import com.onip.cartoonip.data.network.buildRetrofit
import retrofit2.Retrofit

object AppContainer {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var captureStore: CaptureStore
        private set

    lateinit var appContext: Context
        private set

    private var retrofit: Retrofit? = null
    private var cachedBaseUrl: String? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        sessionManager = SessionManager(appContext)
        captureStore = CaptureStore(appContext)
        initialized = true
    }

    fun apiFor(baseUrl: String): ApiServices {
        if (retrofit == null || cachedBaseUrl != baseUrl) {
            retrofit = buildRetrofit(baseUrl) { sessionManager.session.value?.token }
            cachedBaseUrl = baseUrl
        }
        return ApiServices(retrofit!!)
    }

    fun api(): ApiServices {
        val baseUrl = sessionManager.session.value?.baseUrl
            ?: error("Aucune session active : appelez apiFor() avant la connexion.")
        return apiFor(baseUrl)
    }
}
