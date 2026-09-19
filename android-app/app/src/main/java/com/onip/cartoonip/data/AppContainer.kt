package com.onip.cartoonip.data

import android.content.Context
import com.onip.cartoonip.data.network.ApiServices
import com.onip.cartoonip.data.network.buildRetrofit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Retrofit

object AppContainer {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var captureStore: CaptureStore
        private set

    lateinit var appContext: Context
        private set

    // Mise en cache légère de la photo de profil de l'agent connecté (affichée dans la barre du
    // haut) — pas persistée : rechargée depuis /api/me au démarrage et mise à jour après upload.
    private val _agentPhoto = MutableStateFlow<String?>(null)
    val agentPhoto: StateFlow<String?> = _agentPhoto.asStateFlow()

    fun setAgentPhoto(photoDataUrl: String?) {
        _agentPhoto.value = photoDataUrl
    }

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
