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

/** Site importé (tableau de bord : Zones > Sites) appartenant à la zone de l'agent connecté —
 *  renvoyé par GET /api/me/zone-sites. Dans la fiche de repérage du chef d'équipe, choisir un
 *  nom de site remplit d'office province, ville/territoire, commune et adresse. */
data class ZoneSiteOption(
    val id: String,
    val nomSite: String,
    val adresse: String? = null,
    val province: String? = null,
    val villeTerritoire: String? = null,
    val commune: String? = null,
    val zoneCode: String? = null,
)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
