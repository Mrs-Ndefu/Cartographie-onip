package com.onip.cartoonip.ui.reperage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.SiteSyncRepository
import com.onip.cartoonip.data.model.AgentZone
import com.onip.cartoonip.data.model.CapturedSite
import com.onip.cartoonip.data.model.ZoneSiteOption
import java.text.Normalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Liste des fiches de repérage de site enregistrées sur l'appareil — même principe que
 * JournalViewModel (ménages), sans filtre "aujourd'hui" : une fiche de site reste pertinente
 * tant qu'elle n'a pas été traitée. */
/** Site de la zone du chef d'équipe et son état : visité si une fiche porte ce nom sur cet
 *  appareil ([localFiche]) ou déjà sur le serveur ([ZoneSiteOption.visite]). */
data class ZoneSiteVisit(val option: ZoneSiteOption, val localFiche: CapturedSite?) {
    val visited: Boolean get() = option.visite || localFiche != null
}

class ReperageViewModel : ViewModel() {

    // Chef d'équipe : la liste du repérage est celle des sites de sa zone (importés au tableau
    // de bord), chacun « À visiter » puis « Visité » une fois sa fiche remplie.
    val showZoneSites: Boolean = AppContainer.agentRole.value == "CHEF_EQUIPE"
    val agentZone: StateFlow<AgentZone?> = AppContainer.agentZone

    val sites: StateFlow<List<CapturedSite>> = AppContainer.siteStore.sites
        .map { list -> list.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val zoneVisits: StateFlow<List<ZoneSiteVisit>> = combine(AppContainer.zoneSites, AppContainer.siteStore.sites) { options, fiches ->
        options.map { option ->
            ZoneSiteVisit(option, fiches.firstOrNull { nameKey(it.nomSite) == nameKey(option.nomSite) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Fiches de l'appareil qui ne correspondent à aucun site de la zone (ex. remplies avant
    // l'import des sites) : affichées sous la liste pour ne rien perdre.
    val otherFiches: StateFlow<List<CapturedSite>> = combine(AppContainer.zoneSites, sites) { options, fiches ->
        val names = options.map { nameKey(it.nomSite) }.toSet()
        fiches.filter { nameKey(it.nomSite) !in names }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (showZoneSites) {
            viewModelScope.launch { AppContainer.refreshZoneSites() }
        }
    }

    private val _syncingIds = MutableStateFlow<Set<String>>(emptySet())
    val syncingIds: StateFlow<Set<String>> = _syncingIds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun retrySync(site: CapturedSite) {
        _syncingIds.value = _syncingIds.value + site.id
        viewModelScope.launch {
            try {
                SiteSyncRepository.sync(site)
            } catch (e: Exception) {
                _errorMessage.value = "Échec de la synchronisation pour ${site.displayName} : vérifiez la connexion."
            } finally {
                _syncingIds.value = _syncingIds.value - site.id
            }
        }
    }

    fun syncAllPending() {
        sites.value.filter { it.syncedAt == null }.forEach { retrySync(it) }
    }

    fun delete(site: CapturedSite) {
        AppContainer.siteStore.delete(site.id)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun showMessage(message: String) {
        _errorMessage.value = message
    }
}

// Majuscules, sans accents ni espaces superflus, pour reconnaître le même nom de site.
private fun nameKey(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("\\s+"), " ")
        .uppercase()
