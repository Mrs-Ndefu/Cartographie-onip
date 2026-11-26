package com.onip.cartoonip.data.model

data class CapturedMember(
    val nom: String,
    val postnom: String,
    val prenom: String,
    val dateNaissance: String = "",
    val sexe: String? = null,
    val relation: String = "",
)

/** Un foyer capturé sur l'appareil — persistant localement avant/après synchronisation. */
data class CapturedHousehold(
    val id: String,
    val codeMenage: String,
    val chefNom: String,
    val chefPostnom: String,
    val chefPrenom: String,
    val chefDateNaissance: String = "",
    val chefSexe: String? = null,
    val province: String = "",
    val ville: String,
    val commune: String,
    val quartier: String,
    val rue: String,
    val numero: String,
    val immeuble: String,
    val appartement: String = "",
    val etage: String = "",
    val membres: List<CapturedMember> = emptyList(),
    // Nombre de membres déclaré, chef compris. Null pour les foyers enregistrés avant ce champ
    // (on retombe alors sur chef + fiches membres).
    val nombreMembres: Int? = null,
    val latitude: Double?,
    val longitude: Double?,
    val locationPrecision: Double?,
    val photoPaths: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null,
    val photoSyncedAt: String? = null,
) {
    val chefFullName: String
        get() = listOf(chefNom, chefPostnom, chefPrenom).filter { it.isNotBlank() }.joinToString(" ")

    val isFullySynced: Boolean
        get() = syncedAt != null && (photoPaths.isEmpty() || photoSyncedAt != null)

    /** Complet quand chef de ménage (nom, postnom, prénom, date de naissance, sexe), adresse
     * (province, ville, commune, quartier, rue, numéro) et GPS sont tous renseignés. Ne sont pas
     * exigés : la photo, l'immeuble et l'étage (maisons basses sans immeuble ni étage) — ces
     * champs restent saisissables. Sinon le foyer est un brouillon côté serveur (affiché
     * "Incomplet" dans le tableau de bord). */
    val isComplete: Boolean
        get() = chefNom.isNotBlank() && chefPostnom.isNotBlank() && chefPrenom.isNotBlank() &&
            chefDateNaissance.isNotBlank() && chefSexe != null &&
            province.isNotBlank() && ville.isNotBlank() && commune.isNotBlank() && quartier.isNotBlank() &&
            rue.isNotBlank() && numero.isNotBlank() &&
            latitude != null && longitude != null
}

/** Nombre max de photos de fiche capturables par foyer (galerie). */
const val MAX_HOUSEHOLD_PHOTOS = 4
