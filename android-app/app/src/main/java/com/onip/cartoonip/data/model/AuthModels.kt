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
    // Repris séparément de zonePlace (qui les concatène pour l'affichage) : utilisés pour
    // restreindre les listes province/ville/commune de la saisie terrain (cf. CaptureScreen).
    val zoneProvince: String? = null,
    val zoneVille: String? = null,
    val zoneCommunes: List<String>? = null,
)

/** Zone d'affectation de l'agent connecté : affichée sur l'écran de saisie et le profil, et sert
 *  aussi à restreindre les listes province/ville/commune à cette zone (cf. CaptureScreen). */
data class AgentZone(
    val code: String,
    val place: String,
    val province: String? = null,
    val ville: String? = null,
    val communes: List<String> = emptyList(),
)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
