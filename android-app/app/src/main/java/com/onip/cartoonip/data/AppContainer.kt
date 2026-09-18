package com.onip.cartoonip.data

import android.content.Context
import com.onip.cartoonip.data.network.ApiServices
import com.onip.cartoonip.data.network.buildRetrofit
import retrofit2.Retrofit

/**
 * Minimal hand-rolled service locator (no DI framework) — the app has few enough screens that
 * Hilt would add more ceremony than it saves. [api] rebuilds the Retrofit client only when the
 * backend URL changes; the auth token itself is read fresh on every request (see NetworkModule).
 */
object AppContainer {

    lateinit var sessionManager: SessionManager
        private set

    private var retrofit: Retrofit? = null
    private var cachedBaseUrl: String? = null

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        sessionManager = SessionManager(context.applicationContext)
        initialized = true
    }

    /** Api services bound to [baseUrl], with the current session's token (if any) attached. */
    fun apiFor(baseUrl: String): ApiServices {
        if (retrofit == null || cachedBaseUrl != baseUrl) {
            retrofit = buildRetrofit(baseUrl) { sessionManager.session.value?.token }
            cachedBaseUrl = baseUrl
        }
        return ApiServices(retrofit!!)
    }

    /** Api services for the currently logged-in session. Throws if nobody is logged in. */
    fun api(): ApiServices {
        val baseUrl = sessionManager.session.value?.baseUrl
            ?: error("Aucune session active : appelez apiFor() avant la connexion.")
        return apiFor(baseUrl)
    }
}
