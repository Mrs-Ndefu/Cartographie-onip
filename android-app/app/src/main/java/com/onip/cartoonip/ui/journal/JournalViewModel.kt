package com.onip.cartoonip.ui.journal

import com.onip.cartoonip.ui.common.PlaceFilter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
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

    // Filtre de lieu choisi dans la barre de filtres (cf. PlaceFilterBar).
    private val _placeFilter = MutableStateFlow(PlaceFilter())
    val placeFilter: StateFlow<PlaceFilter> = _placeFilter.asStateFlow()

    fun setPlaceFilter(filter: PlaceFilter) {
        _placeFilter.value = filter
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun delete(household: CapturedHousehold) {
        AppContainer.captureStore.delete(household.id)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun showMessage(message: String) {
        _errorMessage.value = message
    }
}
