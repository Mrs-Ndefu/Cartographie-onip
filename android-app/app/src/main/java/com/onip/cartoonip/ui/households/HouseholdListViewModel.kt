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

data class HouseholdListUiState(
    val households: List<HouseholdDto> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

class HouseholdListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdListUiState())
    val uiState: StateFlow<HouseholdListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = HouseholdListUiState(isLoading = true)
        viewModelScope.launch {
            fetchPage(page = 0, append = false)
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.value = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            fetchPage(page = state.page + 1, append = true)
        }
    }

    private suspend fun fetchPage(page: Int, append: Boolean) {
        try {
            val result = AppContainer.api().households.list(page = page)
            _uiState.value = _uiState.value.copy(
                households = if (append) _uiState.value.households + result.content else result.content,
                page = result.number,
                hasMore = !result.last,
                isLoading = false,
                isLoadingMore = false,
                error = null,
            )
        } catch (e: IOException) {
            _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false, error = "Impossible de joindre le serveur.")
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false, error = "Erreur lors du chargement des ménages.")
        }
    }
}
