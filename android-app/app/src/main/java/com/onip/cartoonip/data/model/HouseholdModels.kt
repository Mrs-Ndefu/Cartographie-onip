package com.onip.cartoonip.data.model

import com.google.gson.annotations.SerializedName

enum class HouseholdStatus {
    @SerializedName("brouillon") BROUILLON,
    @SerializedName("complet") COMPLET,
    @SerializedName("a_verifier") A_VERIFIER,
}

enum class Sexe { M, F }

data class PersonDto(
    val nom: String?,
    val postnom: String?,
    val prenom: String?,
    val dateNaissance: String?,
    val sexe: Sexe?,
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

data class HouseholdDto(
    val id: String,
    val codeMenage: String?,
    val nombreMembres: Int?,
    val chef: PersonDto?,
    val membres: List<PersonDto> = emptyList(),
    val address: AddressDto?,
    val location: GeoLocationDto?,
    val meta: FormMetaDto?,
    val status: HouseholdStatus,
    val agentUsername: String?,
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String?,
)

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val last: Boolean,
)
