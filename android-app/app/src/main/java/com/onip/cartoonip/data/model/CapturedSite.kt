package com.onip.cartoonip.data.model

/**
 * Une fiche de repérage de site (bâtiment ou terrain candidat pour un bureau ONIP), capturée sur
 * l'appareil — remplace l'ancien ReperagePoint (simple marqueur GPS, jamais synchronisé). Reprend
 * un à un les champs de la fiche papier "FICHE REPERAGE DES SITES". Rien n'est obligatoire :
 * l'agent peut sauvegarder et synchroniser une fiche partiellement remplie, puis la compléter.
 *
 * Les champs à choix (typeSite, proprieteStatut, optionOccupation, typeInfrastructure,
 * etatBatiment, accesRoutier, stabiliteSnel, connectiviteInternet, avisGlobal) stockent
 * directement le nom de la constante de l'enum côté backend (ex. "BATIMENT_EXISTANT"), en
 * majuscules — pas de mapping supplémentaire nécessaire à la synchronisation.
 */
data class CapturedSite(
    val id: String,

    // I. Identification administrative du site
    val province: String = "",
    val ville: String = "",
    val commune: String = "",
    val quartierGroupement: String = "",
    val villageAvenueRue: String = "",
    val nomSite: String = "",
    val numeroTelephone: String = "",
    val serieTelephoneGps: String = "",

    // II. Coordonnées géographiques (GPS)
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val gpsPrecision: Double? = null,

    // III. Type de site
    val typeSite: String? = null,
    val typeSiteAutre: String = "",

    // IV. Statut juridique et propriété
    val proprieteStatut: String? = null,
    val optionOccupation: String? = null,
    val disponibiliteImmediate: Boolean? = null,
    val delaiEstime: String = "",
    val proprietaireNomContact: String = "",

    // V. Caractéristiques physiques et infrastructures
    val typeInfrastructure: String? = null,
    val typeInfrastructureAutre: String = "",
    val superficieBatimentM2: Double? = null,
    val superficieTerrainM2: Double? = null,
    val nombrePieces: Int? = null,
    val etatBatiment: String? = null,
    val etatBatimentObservations: String = "",

    // VI. Commodité et accès techniques
    val accesRoutier: String? = null,
    val raccordeSnel: Boolean? = null,
    val stabiliteSnel: String? = null,
    val groupeElectrogeneSolaire: Boolean? = null,
    val nonElectrifie: Boolean? = null,
    val regideso: Boolean? = null,
    val sanitaires: Boolean? = null,
    val connectiviteInternet: String? = null,
    val reseauxDisponibles: String = "",
    val clotureSecurise: Boolean? = null,
    val proximitePolice: Boolean? = null,
    val distancePoliceMetres: Int? = null,

    // VII. Évaluation et recommandations de l'équipe de terrain
    val avisGlobal: String? = null,
    val travauxAmenagement: String = "",
    val motifDefavorable: String = "",
    val observationsParticulieres: String = "",
    val nomSignatureEnqueteur: String = "",
    val dateEvaluation: String = "",

    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null,
) {
    /** Résumé affiché dans le journal — nom du site s'il est renseigné, sinon la localisation. */
    val displayName: String
        get() = nomSite.ifBlank {
            listOf(commune, ville).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Site sans nom" }
        }
}
