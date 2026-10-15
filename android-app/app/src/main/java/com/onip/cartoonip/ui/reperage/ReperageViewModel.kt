package com.onip.cartoonip.ui.reperage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.GpsResult
import com.onip.cartoonip.data.LocationHelper
import com.onip.cartoonip.data.model.ReperagePoint
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReperageUiState(
    val locating: Boolean = false,
    val locationError: String? = null,
)

class ReperageViewModel : ViewModel() {

    private val locationHelper = LocationHelper(AppContainer.appContext)

    private val _uiState = MutableStateFlow(ReperageUiState())
    val uiState: StateFlow<ReperageUiState> = _uiState.asStateFlow()

    val points = AppContainer.reperageStore.points

    /** Relève la position actuelle et l'ajoute à la liste des points à visiter. */
    fun markHere() {
        if (_uiState.value.locating) return
        _uiState.value = _uiState.value.copy(locating = true, locationError = null)
        viewModelScope.launch {
            val fix: GpsResult? = locationHelper.bestFix() ?: locationHelper.recentLastKnown()
            if (fix == null) {
                _uiState.value = _uiState.value.copy(locating = false, locationError = "Position GPS indisponible — réessayez à l'extérieur.")
                return@launch
            }
            AppContainer.reperageStore.add(
                ReperagePoint(
                    id = UUID.randomUUID().toString(),
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    locationPrecision = fix.accuracyMeters.toDouble(),
                    createdAt = Instant.now().toString(),
                ),
            )
            _uiState.value = _uiState.value.copy(locating = false)
        }
    }

    fun setVisite(id: String, visite: Boolean) = AppContainer.reperageStore.setVisite(id, visite)

    fun updateNote(id: String, note: String) = AppContainer.reperageStore.updateNote(id, note)

    fun delete(id: String) = AppContainer.reperageStore.delete(id)
}
