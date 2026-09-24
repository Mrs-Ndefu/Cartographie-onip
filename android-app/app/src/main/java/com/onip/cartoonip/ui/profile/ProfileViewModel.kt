package com.onip.cartoonip.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.model.AgentDto
import com.onip.cartoonip.data.model.ChangePasswordRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val MAX_PHOTO_BYTES = 2 * 1024 * 1024
private const val MAX_PHOTO_DIMENSION = 800

// Les photos d'un appareil photo moderne dépassent souvent la limite de 2 Mo du serveur : on
// réduit à MAX_PHOTO_DIMENSION px (largement suffisant pour un avatar) et on ré-encode en JPEG.
private fun downscaleToJpeg(context: Context, uri: Uri): ByteArray? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_PHOTO_DIMENSION) {
        sampleSize *= 2
    }
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    } ?: return null

    val scale = MAX_PHOTO_DIMENSION.toFloat() / maxOf(decoded.width, decoded.height)
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
    } else {
        decoded
    }
    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val agent: AgentDto? = null,
    val photoUploading: Boolean = false,
    val photoError: String? = null,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordLoading: Boolean = false,
    val passwordError: String? = null,
    val passwordSuccess: String? = null,
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            try {
                val agent = AppContainer.api().agent.me()
                _uiState.value = _uiState.value.copy(isLoading = false, agent = agent)
                AppContainer.setAgentPhoto(agent.photoDataUrl)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onCurrentPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(currentPassword = value, passwordError = null, passwordSuccess = null)
    }

    fun onNewPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value, passwordError = null, passwordSuccess = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, passwordError = null, passwordSuccess = null)
    }

    fun changePassword() {
        val state = _uiState.value
        if (state.newPassword.length < 6) {
            _uiState.value = state.copy(passwordError = "Le nouveau mot de passe doit contenir au moins 6 caractères.")
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.value = state.copy(passwordError = "Les deux mots de passe ne correspondent pas.")
            return
        }

        _uiState.value = state.copy(passwordLoading = true, passwordError = null, passwordSuccess = null)
        viewModelScope.launch {
            try {
                val agent = AppContainer.api().agent.changePassword(
                    ChangePasswordRequest(state.currentPassword, state.newPassword),
                )
                _uiState.value = _uiState.value.copy(
                    agent = agent,
                    passwordLoading = false,
                    passwordSuccess = "Mot de passe mis à jour.",
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = "",
                )
            } catch (e: HttpException) {
                val message = if (e.code() == 400) "Mot de passe actuel incorrect." else "Erreur serveur (${e.code()})."
                _uiState.value = _uiState.value.copy(passwordLoading = false, passwordError = message)
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    passwordLoading = false,
                    passwordError = "Impossible de joindre le serveur. Vérifiez la connexion.",
                )
            }
        }
    }

    fun uploadPhoto(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(photoUploading = true, photoError = null)
        viewModelScope.launch {
            try {
                val contentType = context.contentResolver.getType(uri)
                if (contentType == null || !contentType.startsWith("image/")) {
                    _uiState.value = _uiState.value.copy(photoUploading = false, photoError = "Le fichier doit être une image.")
                    return@launch
                }

                val bytes = withContext(Dispatchers.IO) { downscaleToJpeg(context, uri) }
                if (bytes == null) {
                    _uiState.value = _uiState.value.copy(photoUploading = false, photoError = "Impossible de lire le fichier.")
                    return@launch
                }
                if (bytes.size > MAX_PHOTO_BYTES) {
                    _uiState.value = _uiState.value.copy(
                        photoUploading = false,
                        photoError = "La photo dépasse la taille maximale autorisée (2 Mo).",
                    )
                    return@launch
                }

                val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "profile.jpg", body)
                val agent = AppContainer.api().agent.uploadPhoto(part)
                _uiState.value = _uiState.value.copy(photoUploading = false, agent = agent)
                AppContainer.setAgentPhoto(agent.photoDataUrl)
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(photoUploading = false, photoError = "Envoi de la photo impossible (${e.code()}).")
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(photoUploading = false, photoError = "Envoi de la photo impossible — vérifiez la connexion.")
            }
        }
    }

    fun logout() {
        AppContainer.sessionManager.clear()
        AppContainer.setAgentPhoto(null)
    }
}
