package com.onip.cartoonip.data

import android.content.Context
import com.onip.cartoonip.data.model.AgentDto
import com.onip.cartoonip.data.model.AgentZone
import com.onip.cartoonip.data.model.ZoneSiteOption
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    lateinit var siteStore: SiteStore
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

    // Rôle de l'agent connecté — restreint l'accès au Repérage de site sur la page d'accueil
    // (cf. HomeScreen) : CHEF_EQUIPE (et admin-tier) y a accès, un AGENT non.
    private val _agentRole = MutableStateFlow<String?>(null)
    val agentRole: StateFlow<String?> = _agentRole.asStateFlow()

    fun setAgentRole(role: String?) {
        _agentRole.value = role
        sessionManager.role = role
    }

    // Zone d'affectation de l'agent connecté : rechargée depuis /api/me (l'ADMIN ou le superviseur
    // peut la changer à tout moment) et gardée en session pour rester visible hors connexion.
    private val _agentZone = MutableStateFlow<AgentZone?>(null)
    val agentZone: StateFlow<AgentZone?> = _agentZone.asStateFlow()

    fun setAgentZone(code: String?, place: String?, province: String? = null, ville: String? = null, communes: List<String> = emptyList()) {
        _agentZone.value = if (code.isNullOrBlank()) null else AgentZone(code, place.orEmpty(), province, ville, communes)
        sessionManager.zoneCode = code
        sessionManager.zonePlace = place
        sessionManager.zoneProvince = province
        sessionManager.zoneVille = ville
        sessionManager.zoneCommunes = communes
    }

    // Sites de la zone de l'agent (fiche de repérage du chef d'équipe) : chargés depuis le cache
    // au démarrage, puis rafraîchis depuis le serveur à l'ouverture de la fiche.
    private val _zoneSites = MutableStateFlow<List<ZoneSiteOption>>(emptyList())
    val zoneSites: StateFlow<List<ZoneSiteOption>> = _zoneSites.asStateFlow()

    /** Recharge les sites de la zone depuis le serveur ; sans réseau, garde la dernière liste. */
    suspend fun refreshZoneSites() {
        val sites = runCatching { api().agent.zoneSites() }.getOrNull() ?: return
        _zoneSites.value = sites
        sessionManager.zoneSitesJson = Gson().toJson(sites)
    }

    fun clearZoneSites() {
        _zoneSites.value = emptyList()
    }

    private fun loadCachedZoneSites(): List<ZoneSiteOption> {
        val json = sessionManager.zoneSitesJson ?: return emptyList()
        return runCatching {
            Gson().fromJson<List<ZoneSiteOption>>(json, object : TypeToken<List<ZoneSiteOption>>() {}.type)
        }.getOrNull().orEmpty()
    }

    // Jeton refusé par le serveur : on ferme la session (l'écran de connexion s'affiche, cf.
    // CartoOnipNavHost). Les foyers enregistrés sur l'appareil ne sont pas touchés ; leur
    // synchronisation reprend après la reconnexion.
    private fun onSessionExpired() {
        if (sessionManager.session.value == null) return
        sessionManager.expire()
        _agentPhoto.value = null
        _agentZone.value = null
        _agentRole.value = null
        _zoneSites.value = emptyList()
    }

    /** Met à jour photo, zone et rôle d'après la fiche de l'agent renvoyée par le serveur. */
    fun applyAgent(agent: AgentDto) {
        setAgentPhoto(agent.photoDataUrl)
        setAgentZone(agent.zoneCode, agent.zonePlace, agent.zoneProvince, agent.zoneVille, agent.zoneCommunes.orEmpty())
        setAgentRole(agent.role)
    }

    private var retrofit: Retrofit? = null
    private var cachedBaseUrl: String? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        sessionManager = SessionManager(appContext)
        captureStore = CaptureStore(appContext)
        siteStore = SiteStore(appContext)
        sessionManager.zoneCode?.let {
            _agentZone.value = AgentZone(
                it, sessionManager.zonePlace.orEmpty(),
                sessionManager.zoneProvince, sessionManager.zoneVille, sessionManager.zoneCommunes,
            )
        }
        _agentRole.value = sessionManager.role
        _zoneSites.value = loadCachedZoneSites()
        initialized = true
    }

    fun apiFor(baseUrl: String): ApiServices {
        if (retrofit == null || cachedBaseUrl != baseUrl) {
            retrofit = buildRetrofit(
                baseUrl,
                tokenProvider = { sessionManager.session.value?.token },
                onUnauthorized = ::onSessionExpired,
            )
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
