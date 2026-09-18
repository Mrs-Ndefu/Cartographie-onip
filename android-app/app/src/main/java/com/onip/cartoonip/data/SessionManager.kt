package com.onip.cartoonip.data

import android.content.Context
import android.content.SharedPreferences
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

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "carto_onip_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<Session?> = _session.asStateFlow()

    var lastBaseUrl: String
        get() = prefs.getString(KEY_LAST_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_BASE_URL, value).apply()

    fun save(baseUrl: String, token: String, agent: AgentDto) {
        prefs.edit()
            .putString(KEY_BASE_URL, baseUrl)
            .putString(KEY_TOKEN, token)
            .putString(KEY_AGENT_ID, agent.id)
            .putString(KEY_USERNAME, agent.username)
            .putString(KEY_FULL_NAME, agent.fullName)
            .putString(KEY_LAST_BASE_URL, baseUrl)
            .apply()
        _session.value = loadSession()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_BASE_URL).remove(KEY_TOKEN).remove(KEY_AGENT_ID)
            .remove(KEY_USERNAME).remove(KEY_FULL_NAME)
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
        private const val KEY_TOKEN = "token"
        private const val KEY_AGENT_ID = "agent_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_LAST_BASE_URL = "last_base_url"
    }
}
