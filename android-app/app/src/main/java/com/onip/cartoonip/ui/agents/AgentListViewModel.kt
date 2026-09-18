package com.onip.cartoonip.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.model.AgentDto
import com.onip.cartoonip.data.model.AgentRole
import com.onip.cartoonip.data.model.ChangeRoleRequest
import com.onip.cartoonip.data.model.CreateAgentRequest
import com.onip.cartoonip.data.model.ResetPasswordRequest
import com.onip.cartoonip.data.model.SetActiveRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class AgentListUiState(
    val agents: List<AgentDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionError: String? = null,
)

class AgentListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AgentListUiState())
    val uiState: StateFlow<AgentListUiState> = _uiState.asStateFlow()

    val currentUsername: String get() = AppContainer.sessionManager.session.value?.username.orEmpty()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val agents = AppContainer.api().agents.list()
                _uiState.value = _uiState.value.copy(isLoading = false, agents = agents)
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Impossible de joindre le serveur.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erreur lors du chargement des agents.")
            }
        }
    }

    fun createAgent(username: String, password: String, fullName: String, role: AgentRole, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                AppContainer.api().agents.create(CreateAgentRequest(username, password, fullName, role))
                refresh()
                onDone(true)
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(actionError = "Création refusée (identifiant déjà utilisé ou données invalides).")
                onDone(false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(actionError = "Échec de la création de l'agent.")
                onDone(false)
            }
        }
    }

    fun setActive(agent: AgentDto, active: Boolean) {
        viewModelScope.launch {
            try {
                AppContainer.api().agents.setActive(agent.id, SetActiveRequest(active))
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(actionError = "Action refusée.")
            }
        }
    }

    fun changeRole(agent: AgentDto, role: AgentRole) {
        viewModelScope.launch {
            try {
                AppContainer.api().agents.changeRole(agent.id, ChangeRoleRequest(role))
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(actionError = "Changement de rôle refusé.")
            }
        }
    }

    fun resetPassword(agent: AgentDto, newPassword: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                AppContainer.api().agents.resetPassword(agent.id, ResetPasswordRequest(newPassword))
                onDone(true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(actionError = "Échec de la réinitialisation du mot de passe.")
                onDone(false)
            }
        }
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }
}
