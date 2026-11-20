package com.onip.cartoonip.data

import com.onip.cartoonip.data.model.CapturedHousehold
import com.onip.cartoonip.data.model.CapturedSite
import com.onip.cartoonip.data.model.LoginRequest
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant

/**
 * Envoi des données au serveur, toujours précédé d'une authentification : l'agent ressaisit son
 * mot de passe, vérifié par le serveur (nouvelle connexion, qui renouvelle aussi la session),
 * avant tout envoi. Il n'y a plus de synchronisation automatique (ni à l'enregistrement, ni au
 * retour du réseau) : les données restent sur l'appareil, en attente, jusqu'à cet envoi.
 *
 * Rétention : une donnée synchronisée est effacée de l'appareil 24 h après son envoi complet
 * ([purgeSynced]) ; une donnée en attente n'est jamais effacée. Le nombre de données effacées est
 * gardé (SessionManager) pour que le tableau de bord de l'accueil affiche le vrai total envoyé.
 */
object SecureSync {

    private val RETENTION: Duration = Duration.ofHours(24)

    sealed class Check {
        data object Ok : Check()
        data object WrongPassword : Check()
        data class Failed(val message: String) : Check()
    }

    data class Report(val sent: Int, val failed: Int, val firstError: String?) {
        val message: String
            get() = when {
                sent == 0 && failed == 0 -> "Rien à envoyer."
                failed == 0 -> "$sent donnée(s) envoyée(s) au serveur."
                else -> "$sent envoyée(s), $failed en échec" + (firstError?.let { " ($it)" } ?: "") + "."
            }
    }

    /** Vérifie le mot de passe de l'agent connecté auprès du serveur. */
    suspend fun verifyPassword(password: String): Check {
        val session = AppContainer.sessionManager.session.value ?: return Check.Failed("Session expirée : reconnectez-vous.")
        if (password.isBlank()) return Check.WrongPassword
        return try {
            val response = AppContainer.apiFor(session.baseUrl).auth.login(LoginRequest(session.username, password))
            // Le jeton renouvelé sert aux envois qui suivent.
            AppContainer.sessionManager.save(session.baseUrl, response.token, response.agent)
            AppContainer.applyAgent(response.agent)
            Check.Ok
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) Check.WrongPassword
            else Check.Failed("Serveur indisponible (erreur ${e.code()}).")
        } catch (e: IOException) {
            Check.Failed("Pas de connexion au serveur : vérifiez le réseau.")
        }
    }

    /** Vérifie le mot de passe puis envoie les foyers et fiches donnés. */
    suspend fun send(
        password: String,
        households: List<CapturedHousehold> = emptyList(),
        sites: List<CapturedSite> = emptyList(),
    ): Pair<Check, Report?> {
        val check = verifyPassword(password)
        if (check != Check.Ok) return check to null
        var sent = 0
        var failed = 0
        var firstError: String? = null
        households.forEach { h ->
            try {
                SyncRepository.sync(h)
                sent++
            } catch (e: Exception) {
                failed++
                if (firstError == null) firstError = e.message
            }
        }
        sites.forEach { s ->
            try {
                SiteSyncRepository.sync(s)
                sent++
            } catch (e: Exception) {
                failed++
                if (firstError == null) firstError = e.message
            }
        }
        return check to Report(sent, failed, firstError)
    }

    /** Toutes les données en attente de l'appareil (foyers et fiches de repérage). */
    suspend fun sendAllPending(password: String): Pair<Check, Report?> = send(
        password,
        AppContainer.captureStore.households.value.filterNot { it.isFullySynced },
        AppContainer.siteStore.sites.value.filter { it.syncedAt == null },
    )

    /**
     * Efface les données envoyées depuis plus de 24 h (photos comprises). Les données en attente
     * ne sont jamais effacées. Appelé au démarrage et à l'ouverture de l'accueil.
     */
    fun purgeSynced(now: Instant = Instant.now()) {
        val limit = now.minus(RETENTION)
        val oldHouseholds = AppContainer.captureStore.households.value.filter { h ->
            h.isFullySynced && (fullySyncedAt(h)?.isBefore(limit) == true)
        }
        oldHouseholds.forEach { h ->
            h.photoPaths.forEach { runCatching { File(it).delete() } }
            AppContainer.captureStore.delete(h.id)
        }
        val oldSites = AppContainer.siteStore.sites.value.filter { s ->
            parse(s.syncedAt)?.isBefore(limit) == true
        }
        oldSites.forEach { AppContainer.siteStore.delete(it.id) }
        if (oldHouseholds.isNotEmpty() || oldSites.isNotEmpty()) {
            AppContainer.sessionManager.addPurged(oldHouseholds.size, oldSites.size)
        }
    }

    // Envoi complet d'un foyer : champs puis photos (la plus tardive des deux dates).
    private fun fullySyncedAt(h: CapturedHousehold): Instant? {
        val fields = parse(h.syncedAt) ?: return null
        val photos = parse(h.photoSyncedAt)
        return if (photos != null && photos.isAfter(fields)) photos else fields
    }

    private fun parse(value: String?): Instant? = value?.let { runCatching { Instant.parse(it) }.getOrNull() }
}
