package com.onip.cartoonip.data.model

// Miroir du contrat JSON attendu par POST /api/households/sync côté backend
// (HouseholdSyncItem / PersonDto / AddressDto / GeoLocationDto / FormMetaDto).
// Le "statut" est envoyé en minuscules ("complet") — le backend le convertit lui-même vers
// l'enum HouseholdStatus (HouseholdStatus.fromValue), Gson n'a pas besoin de le savoir.

data class HouseholdSyncRequest(val households: List<HouseholdSyncItem>)

data class HouseholdSyncItem(
    val id: String,
    val codeMenage: String?,
    val nombreMembres: Int?,
    val chef: PersonDto,
    val membres: List<PersonDto> = emptyList(),
    val address: AddressDto,
    val location: GeoLocationDto?,
    val meta: FormMetaDto,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

data class PersonDto(
    val nom: String?,
    val postnom: String?,
    val prenom: String?,
    val dateNaissance: String?,
    val lieuNaissance: String?,
    val sexe: String?,
    val relation: String?,
)

data class AddressDto(
    val ville: String?,
    val commune: String?,
    val quartier: String?,
    val rue: String?,
    val numero: String?,
    val immeuble: String?,
)

data class GeoLocationDto(
    val latitude: Double?,
    val longitude: Double?,
    val precision: Double?,
    val saisieManuelle: Boolean?,
)

data class FormMetaDto(
    val faitA: String?,
    val dateEncodage: String?,
    val nombreFiches: String?,
    val agentCarthographe: String?,
    val renseignant: String?,
)

data class SyncResponse(
    val accepted: List<String> = emptyList(),
    val rejected: List<SyncRejection> = emptyList(),
)

data class SyncRejection(val id: String, val reason: String)
