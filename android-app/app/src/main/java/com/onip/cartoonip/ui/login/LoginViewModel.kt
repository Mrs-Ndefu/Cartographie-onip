package com.onip.cartoonip.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class LoginUiState(
    val baseUrl: String = AppContainer.sessionManager.lastBaseUrl,
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onBaseUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value, error = null)
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        val baseUrl = normalizeUrl(state.baseUrl)
        if (baseUrl.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Adresse du serveur, identifiant et mot de passe sont requis.")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val response = AppContainer.apiFor(baseUrl).auth.login(LoginRequest(state.username.trim(), state.password))
                AppContainer.sessionManager.save(baseUrl, response.token, response.agent)
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } catch (e: HttpException) {
                val message = if (e.code() == 401) "Identifiant ou mot de passe incorrect." else "Erreur serveur (${e.code()})."
                _uiState.value = _uiState.value.copy(isLoading = false, error = message)
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Impossible de joindre le serveur. Vérifiez l'adresse et la connexion réseau.",
                )
            }
        }
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isBlank()) return trimmed
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
    }
}
