package com.onip.cartoonip.data

import com.onip.cartoonip.data.model.CapturedSite
import com.onip.cartoonip.data.model.SiteSyncItem
import com.onip.cartoonip.data.model.SiteSyncRequest
import java.time.Instant

/**
 * Synchronise une fiche de repérage de site capturée localement — même principe que
 * SyncRepository (ménages) mais sans photo : un seul appel JSON suffit.
 */
object SiteSyncRepository {

    suspend fun sync(site: CapturedSite): CapturedSite {
        val api = AppContainer.api()
        val item = SiteSyncItem(
            id = site.id,
            province = site.province.ifBlank { null },
            ville = site.ville.ifBlank { null },
            commune = site.commune.ifBlank { null },
            quartierGroupement = site.quartierGroupement.ifBlank { null },
            villageAvenueRue = site.villageAvenueRue.ifBlank { null },
            nomSite = site.nomSite.ifBlank { null },
            numeroTelephone = site.numeroTelephone.ifBlank { null },
            serieTelephoneGps = site.serieTelephoneGps.ifBlank { null },
            latitude = site.latitude,
            longitude = site.longitude,
            altitude = site.altitude,
            gpsPrecision = site.gpsPrecision,
            typeSite = site.typeSite,
            typeSiteAutre = site.typeSiteAutre.ifBlank { null },
            proprieteStatut = site.proprieteStatut,
            optionOccupation = site.optionOccupation,
            disponibiliteImmediate = site.disponibiliteImmediate,
            delaiEstime = site.delaiEstime.ifBlank { null },
            proprietaireNomContact = site.proprietaireNomContact.ifBlank { null },
            typeInfrastructure = site.typeInfrastructure,
            typeInfrastructureAutre = site.typeInfrastructureAutre.ifBlank { null },
            superficieBatimentM2 = site.superficieBatimentM2,
            superficieTerrainM2 = site.superficieTerrainM2,
            nombrePieces = site.nombrePieces,
            etatBatiment = site.etatBatiment,
            etatBatimentObservations = site.etatBatimentObservations.ifBlank { null },
            accesRoutier = site.accesRoutier,
            raccordeSnel = site.raccordeSnel,
            stabiliteSnel = site.stabiliteSnel,
            groupeElectrogeneSolaire = site.groupeElectrogeneSolaire,
            nonElectrifie = site.nonElectrifie,
            regideso = site.regideso,
            sanitaires = site.sanitaires,
            connectiviteInternet = site.connectiviteInternet,
            reseauxDisponibles = site.reseauxDisponibles.ifBlank { null },
            clotureSecurise = site.clotureSecurise,
            proximitePolice = site.proximitePolice,
            distancePoliceMetres = site.distancePoliceMetres,
            avisGlobal = site.avisGlobal,
            travauxAmenagement = site.travauxAmenagement.ifBlank { null },
            motifDefavorable = site.motifDefavorable.ifBlank { null },
            observationsParticulieres = site.observationsParticulieres.ifBlank { null },
            nomSignatureEnqueteur = site.nomSignatureEnqueteur.ifBlank { null },
            dateEvaluation = site.dateEvaluation.ifBlank { null },
            createdAt = site.createdAt,
            updatedAt = site.updatedAt,
        )

        val response = api.sites.sync(SiteSyncRequest(listOf(item)))
        if (!response.accepted.contains(site.id)) {
            val reason = response.rejected.firstOrNull { it.id == site.id }?.reason
            error(reason ?: "Synchronisation refusée par le serveur")
        }
        val synced = site.copy(syncedAt = Instant.now().toString())
        AppContainer.siteStore.upsert(synced)
        return synced
    }
}
