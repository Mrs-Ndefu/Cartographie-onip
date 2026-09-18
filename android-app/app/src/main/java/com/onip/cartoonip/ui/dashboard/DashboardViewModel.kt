package com.onip.cartoonip.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.model.DashboardStatsDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

data class DashboardUiState(
    val isLoading: Boolean = true,
    val stats: DashboardStatsDto? = null,
    val error: String? = null,
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val adminFullName: String get() = AppContainer.sessionManager.session.value?.fullName.orEmpty()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val stats = AppContainer.api().dashboard.stats()
                _uiState.value = DashboardUiState(isLoading = false, stats = stats)
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Impossible de joindre le serveur.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erreur lors du chargement des statistiques.")
            }
        }
    }
}
