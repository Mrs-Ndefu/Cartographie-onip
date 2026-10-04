package com.onip.cartoonip.data.model

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    val token: String,
    val expiresInMinutes: Long,
    val agent: AgentDto,
)

data class AgentDto(
    val id: String,
    val username: String,
    val fullName: String,
    val role: String,
    val active: Boolean,
    val photoDataUrl: String? = null,
    // Zone d'affectation : lettre ("A") et lieu ("KINSHASA / KINSHASA / GOMBE") ; null si aucune.
    val zoneCode: String? = null,
    val zonePlace: String? = null,
)

/** Zone d'affectation de l'agent connecté, affichée sur l'écran de saisie et le profil. */
data class AgentZone(val code: String, val place: String)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
