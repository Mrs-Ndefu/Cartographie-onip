package com.onip.cartoonip.ui.households

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.model.HouseholdDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

data class HouseholdDetailUiState(
    val isLoading: Boolean = true,
    val household: HouseholdDto? = null,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
)

class HouseholdDetailViewModel(private val householdId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdDetailUiState())
    val uiState: StateFlow<HouseholdDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val household = AppContainer.api().households.get(householdId)
                _uiState.value = _uiState.value.copy(isLoading = false, household = household)
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Impossible de joindre le serveur.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Ménage introuvable.")
            }
        }
    }

    fun delete() {
        _uiState.value = _uiState.value.copy(isDeleting = true)
        viewModelScope.launch {
            try {
                AppContainer.api().households.delete(householdId)
                _uiState.value = _uiState.value.copy(isDeleting = false, deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isDeleting = false, error = "Échec de la suppression.")
            }
        }
    }
}
