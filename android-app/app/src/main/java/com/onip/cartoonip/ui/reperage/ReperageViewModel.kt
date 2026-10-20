package com.onip.cartoonip.ui.reperage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.SiteSyncRepository
import com.onip.cartoonip.data.model.CapturedSite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Liste des fiches de repérage de site enregistrées sur l'appareil — même principe que
 * JournalViewModel (ménages), sans filtre "aujourd'hui" : une fiche de site reste pertinente
 * tant qu'elle n'a pas été traitée. */
class ReperageViewModel : ViewModel() {

    val sites: StateFlow<List<CapturedSite>> = AppContainer.siteStore.sites
        .map { list -> list.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
}
