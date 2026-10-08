package com.onip.cartoonip.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.onip.cartoonip.data.model.AgentDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Session(
    val baseUrl: String,
    val token: String,
    val agentId: String,
    val username: String,
    val fullName: String,
)

/** Session chiffrée sur l'appareil : URL du serveur, jeton JWT, identité de l'agent connecté. */
class SessionManager(context: Context) {

    // Le Keystore Android ne supporte les clés AES symétriques (requises par MasterKey) qu'à
    // partir de l'API 23 — sur les appareils plus anciens (la tablette terrain tourne en API 21),
    // la génération de la clé plante. On retombe sur des SharedPreferences en clair dans ce cas :
    // moins bien, mais préférable à un crash au démarrage de l'app.
    private val prefs: SharedPreferences = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "carto_onip_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } else {
        context.getSharedPreferences("carto_onip_session_legacy", Context.MODE_PRIVATE)
    }

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<Session?> = _session.asStateFlow()

    var lastBaseUrl: String
        get() = prefs.getString(KEY_LAST_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_BASE_URL, value).apply()

    // Identifiant de l'agent dont la session vient d'expirer : l'écran de connexion le
    // pré-remplit et affiche "session expirée". Effacé à la connexion suivante.
    val expiredUsername: String?
        get() = prefs.getString(KEY_EXPIRED_USERNAME, null)

    /** Session refusée par le serveur : on la ferme en gardant l'identifiant pour la reconnexion. */
    fun expire() {
        val username = _session.value?.username ?: return
        // Enregistré avant de fermer la session : l'écran de connexion, qui s'affiche dès la
        // fermeture, doit déjà le trouver.
        prefs.edit().putString(KEY_EXPIRED_USERNAME, username).commit()
        clear()
    }

    // Dernière zone d'affectation connue, pour l'afficher même sans réseau sur le terrain.
    var zoneCode: String?
        get() = prefs.getString(KEY_ZONE_CODE, null)
        set(value) = prefs.edit().putString(KEY_ZONE_CODE, value).apply()

    var zonePlace: String?
        get() = prefs.getString(KEY_ZONE_PLACE, null)
        set(value) = prefs.edit().putString(KEY_ZONE_PLACE, value).apply()

    var zoneProvince: String?
        get() = prefs.getString(KEY_ZONE_PROVINCE, null)
        set(value) = prefs.edit().putString(KEY_ZONE_PROVINCE, value).apply()

    var zoneVille: String?
        get() = prefs.getString(KEY_ZONE_VILLE, null)
        set(value) = prefs.edit().putString(KEY_ZONE_VILLE, value).apply()

    // Communes de la zone : SharedPreferences ne stocke pas de liste directement, d'où le join
    // sur un séparateur improbable dans un nom de commune.
    var zoneCommunes: List<String>
        get() = prefs.getString(KEY_ZONE_COMMUNES, null)?.split(ZONE_COMMUNES_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = prefs.edit().putString(KEY_ZONE_COMMUNES, value.joinToString(ZONE_COMMUNES_SEPARATOR)).apply()

    fun save(baseUrl: String, token: String, agent: AgentDto) {
        prefs.edit()
            .putString(KEY_BASE_URL, baseUrl)
            .putString(KEY_TOKEN, token)
            .putString(KEY_AGENT_ID, agent.id)
            .putString(KEY_USERNAME, agent.username)
            .putString(KEY_FULL_NAME, agent.fullName)
            .putString(KEY_LAST_BASE_URL, baseUrl)
            .remove(KEY_EXPIRED_USERNAME)
            .apply()
        _session.value = loadSession()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_BASE_URL).remove(KEY_TOKEN).remove(KEY_AGENT_ID)
            .remove(KEY_USERNAME).remove(KEY_FULL_NAME)
            .remove(KEY_ZONE_CODE).remove(KEY_ZONE_PLACE)
            .remove(KEY_ZONE_PROVINCE).remove(KEY_ZONE_VILLE).remove(KEY_ZONE_COMMUNES)
            .apply()
        _session.value = null
    }

    private fun loadSession(): Session? {
        val baseUrl = prefs.getString(KEY_BASE_URL, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val agentId = prefs.getString(KEY_AGENT_ID, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val fullName = prefs.getString(KEY_FULL_NAME, null) ?: return null
        return Session(baseUrl, token, agentId, username, fullName)
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_ZONE_CODE = "zone_code"
        private const val KEY_EXPIRED_USERNAME = "expired_username"
        private const val KEY_ZONE_PLACE = "zone_place"
        private const val KEY_ZONE_PROVINCE = "zone_province"
        private const val KEY_ZONE_VILLE = "zone_ville"
        private const val KEY_ZONE_COMMUNES = "zone_communes"
        private const val ZONE_COMMUNES_SEPARATOR = "|||"
        private const val KEY_TOKEN = "token"
        private const val KEY_AGENT_ID = "agent_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_LAST_BASE_URL = "last_base_url"
    }
}
