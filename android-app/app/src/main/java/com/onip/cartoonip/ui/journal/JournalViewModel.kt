package com.onip.cartoonip.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.SyncRepository
import com.onip.cartoonip.data.model.CapturedHousehold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun isToday(createdAt: String): Boolean {
    val date = runCatching { Instant.parse(createdAt).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
    return date == LocalDate.now()
}

class JournalViewModel : ViewModel() {

    /** Le Journal ne montre que les enregistrements du jour — l'historique complet est dans Aperçu. */
    val households: StateFlow<List<CapturedHousehold>> = AppContainer.captureStore.households
        .map { list -> list.filter { isToday(it.createdAt) }.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _syncingIds = MutableStateFlow<Set<String>>(emptySet())
    val syncingIds: StateFlow<Set<String>> = _syncingIds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun retrySync(household: CapturedHousehold) {
        _syncingIds.value = _syncingIds.value + household.id
        viewModelScope.launch {
            try {
                SyncRepository.sync(household)
            } catch (e: Exception) {
                _errorMessage.value = "Échec de la synchronisation pour ${household.codeMenage} : vérifiez la connexion."
            } finally {
                _syncingIds.value = _syncingIds.value - household.id
            }
        }
    }

    fun syncAllPending() {
        val pending = households.value.filterNot { it.isFullySynced }
        pending.forEach { retrySync(it) }
    }

    fun delete(household: CapturedHousehold) {
        AppContainer.captureStore.delete(household.id)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
