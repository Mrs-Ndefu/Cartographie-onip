package com.onip.cartoonip.data

import com.onip.cartoonip.data.model.AddressDto
import com.onip.cartoonip.data.model.CapturedHousehold
import com.onip.cartoonip.data.model.FormMetaDto
import com.onip.cartoonip.data.model.GeoLocationDto
import com.onip.cartoonip.data.model.HouseholdSyncItem
import com.onip.cartoonip.data.model.HouseholdSyncRequest
import com.onip.cartoonip.data.model.PersonDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.Instant

/**
 * Synchronise un ménage capturé localement : d'abord les champs (sync JSON, comme le fait déjà
 * l'app terrain React), puis les photos (jusqu'à MAX_HOUSEHOLD_PHOTOS, envoyées en un seul appel
 * qui remplace tout le jeu côté serveur) si elles ne sont pas encore envoyées. Les deux étapes
 * sont indépendantes exprès — une coupure réseau entre les deux laisse le ménage "à moitié
 * synchronisé" (syncedAt posé, photoSyncedAt vide), ce que l'écran Journal peut détecter et
 * relancer seul.
 */
object SyncRepository {

    suspend fun sync(household: CapturedHousehold): CapturedHousehold {
        var current = household
        val api = AppContainer.api()

        if (current.syncedAt == null) {
            val item = HouseholdSyncItem(
                id = current.id,
                codeMenage = current.codeMenage,
                nombreMembres = 1 + current.membres.size,
                chef = PersonDto(
                    nom = current.chefNom.ifBlank { null },
                    postnom = current.chefPostnom.ifBlank { null },
                    prenom = current.chefPrenom.ifBlank { null },
                    dateNaissance = current.chefDateNaissance.ifBlank { null },
                    sexe = current.chefSexe,
                    relation = null,
                ),
                membres = current.membres.map {
                    PersonDto(
                        nom = it.nom.ifBlank { null },
                        postnom = it.postnom.ifBlank { null },
                        prenom = it.prenom.ifBlank { null },
                        dateNaissance = it.dateNaissance.ifBlank { null },
                        sexe = it.sexe,
                        relation = it.relation.ifBlank { null },
                    )
                },
                address = AddressDto(
                    current.ville, current.commune, current.quartier, current.rue,
                    current.numero, current.immeuble, current.etage,
                ),
                location = if (current.latitude != null && current.longitude != null) {
                    GeoLocationDto(current.latitude, current.longitude, current.locationPrecision, false)
                } else null,
                meta = FormMetaDto(null, null, null, null, null),
                status = if (current.isComplete) "complet" else "brouillon",
                createdAt = current.createdAt,
                updatedAt = current.updatedAt,
            )

            val response = api.households.sync(HouseholdSyncRequest(listOf(item)))
            if (response.accepted.contains(current.id)) {
                current = current.copy(syncedAt = Instant.now().toString())
                AppContainer.captureStore.upsert(current)
            } else {
                val reason = response.rejected.firstOrNull { it.id == current.id }?.reason
                error(reason ?: "Synchronisation refusée par le serveur")
            }
        }

        if (current.photoPaths.isNotEmpty() && current.photoSyncedAt == null) {
            val parts = current.photoPaths.mapNotNull { path ->
                val file = File(path)
                if (!file.exists()) return@mapNotNull null
                val body = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("files", file.name, body)
            }
            if (parts.isNotEmpty()) {
                api.households.uploadPhotos(current.id, parts)
                current = current.copy(photoSyncedAt = Instant.now().toString())
                AppContainer.captureStore.upsert(current)
            }
        }

        return current
    }
}
