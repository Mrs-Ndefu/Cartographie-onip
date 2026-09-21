package com.onip.cartoonip.data.model

data class CapturedMember(
    val nom: String,
    val postnom: String,
    val prenom: String,
    val dateNaissance: String = "",
    val lieuNaissance: String = "",
    val sexe: String? = null,
    val relation: String = "",
)

/** Un ménage capturé sur l'appareil — persistant localement avant/après synchronisation. */
data class CapturedHousehold(
    val id: String,
    val codeMenage: String,
    val chefNom: String,
    val chefPostnom: String,
    val chefPrenom: String,
    val chefDateNaissance: String = "",
    val chefLieuNaissance: String = "",
    val chefSexe: String? = null,
    val ville: String,
    val commune: String,
    val quartier: String,
    val rue: String,
    val numero: String,
    val immeuble: String,
    val membres: List<CapturedMember> = emptyList(),
    val latitude: Double?,
    val longitude: Double?,
    val locationPrecision: Double?,
    val photoPath: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null,
    val photoSyncedAt: String? = null,
) {
    val chefFullName: String
        get() = listOf(chefNom, chefPostnom, chefPrenom).filter { it.isNotBlank() }.joinToString(" ")

    val isFullySynced: Boolean
        get() = syncedAt != null && (photoPath == null || photoSyncedAt != null)
}
