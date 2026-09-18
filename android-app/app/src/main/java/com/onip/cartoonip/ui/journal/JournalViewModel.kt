package com.onip.cartoonip.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.SyncRepository
import com.onip.cartoonip.data.model.CapturedHousehold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JournalViewModel : ViewModel() {

    val households: StateFlow<List<CapturedHousehold>> = AppContainer.captureStore.households

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
