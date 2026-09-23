package com.onip.cartoonip.data.model

data class CapturedMember(
    val nom: String,
    val postnom: String,
    val prenom: String,
    val dateNaissance: String = "",
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
    val chefSexe: String? = null,
    val ville: String,
    val commune: String,
    val quartier: String,
    val rue: String,
    val numero: String,
    val immeuble: String,
    val etage: String = "",
    val membres: List<CapturedMember> = emptyList(),
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

    /** Complet seulement si absolument tous les champs (obligatoires et facultatifs) sont
     * renseignés, y compris au moins une photo — sinon le ménage est un brouillon côté serveur
     * (affiché "Incomplet" dans le tableau de bord). */
    val isComplete: Boolean
        get() = chefNom.isNotBlank() && chefPostnom.isNotBlank() && chefPrenom.isNotBlank() &&
            chefDateNaissance.isNotBlank() && chefSexe != null &&
            ville.isNotBlank() && commune.isNotBlank() && quartier.isNotBlank() &&
            rue.isNotBlank() && numero.isNotBlank() && immeuble.isNotBlank() && etage.isNotBlank() &&
            latitude != null && longitude != null &&
            photoPaths.isNotEmpty()
}

/** Nombre max de photos de fiche capturables par ménage (galerie). */
const val MAX_HOUSEHOLD_PHOTOS = 4
