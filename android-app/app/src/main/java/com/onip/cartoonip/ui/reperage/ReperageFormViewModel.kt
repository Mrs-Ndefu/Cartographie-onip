package com.onip.cartoonip.ui.reperage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.LocationHelper
import com.onip.cartoonip.data.SiteSyncRepository
import com.onip.cartoonip.data.model.CapturedSite
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * État du formulaire de fiche de repérage de site — un champ par case de FICHE REPERAGE DES
 * SITES.docx. Rien n'est obligatoire : [save] accepte la fiche à tout stade de remplissage,
 * comme le reste de l'app terrain tolère un brouillon synchronisé puis complété plus tard.
 */
data class SiteFormState(
    val siteId: String = UUID.randomUUID().toString(),
    val isEditing: Boolean = false,

    val locating: Boolean = false,
    val locationError: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val gpsPrecision: Double? = null,

    // I. Identification administrative du site
    val province: String = "",
    val ville: String = "",
    val commune: String = "",
    val quartierGroupement: String = "",
    val villageAvenueRue: String = "",
    val nomSite: String = "",
    val numeroTelephone: String = "",
    val serieTelephoneGps: String = "",

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
    val superficieBatiment: String = "",
    val superficieTerrain: String = "",
    val nombrePieces: String = "",
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
    val distancePoliceMetres: String = "",

    // VII. Évaluation et recommandations de l'équipe de terrain
    val avisGlobal: String? = null,
    val travauxAmenagement: String = "",
    val motifDefavorable: String = "",
    val observationsParticulieres: String = "",
    val nomSignatureEnqueteur: String = "",
    val dateEvaluation: String = "",

    val saving: Boolean = false,
    val saveError: String? = null,
)

class ReperageFormViewModel : ViewModel() {

    private val locationHelper = LocationHelper(AppContainer.appContext)

    private val _uiState = MutableStateFlow(SiteFormState())
    val uiState: StateFlow<SiteFormState> = _uiState.asStateFlow()

    fun loadForEdit(id: String) {
        val site = AppContainer.siteStore.get(id) ?: return
        _uiState.value = SiteFormState(
            siteId = site.id,
            isEditing = true,
            latitude = site.latitude,
            longitude = site.longitude,
            altitude = site.altitude,
            gpsPrecision = site.gpsPrecision,
            province = site.province,
            ville = site.ville,
            commune = site.commune,
            quartierGroupement = site.quartierGroupement,
            villageAvenueRue = site.villageAvenueRue,
            nomSite = site.nomSite,
            numeroTelephone = site.numeroTelephone,
            serieTelephoneGps = site.serieTelephoneGps,
            typeSite = site.typeSite,
            typeSiteAutre = site.typeSiteAutre,
            proprieteStatut = site.proprieteStatut,
            optionOccupation = site.optionOccupation,
            disponibiliteImmediate = site.disponibiliteImmediate,
            delaiEstime = site.delaiEstime,
            proprietaireNomContact = site.proprietaireNomContact,
            typeInfrastructure = site.typeInfrastructure,
            typeInfrastructureAutre = site.typeInfrastructureAutre,
            superficieBatiment = site.superficieBatimentM2?.toString() ?: "",
            superficieTerrain = site.superficieTerrainM2?.toString() ?: "",
            nombrePieces = site.nombrePieces?.toString() ?: "",
            etatBatiment = site.etatBatiment,
            etatBatimentObservations = site.etatBatimentObservations,
            accesRoutier = site.accesRoutier,
            raccordeSnel = site.raccordeSnel,
            stabiliteSnel = site.stabiliteSnel,
            groupeElectrogeneSolaire = site.groupeElectrogeneSolaire,
            nonElectrifie = site.nonElectrifie,
            regideso = site.regideso,
            sanitaires = site.sanitaires,
            connectiviteInternet = site.connectiviteInternet,
            reseauxDisponibles = site.reseauxDisponibles,
            clotureSecurise = site.clotureSecurise,
            proximitePolice = site.proximitePolice,
            distancePoliceMetres = site.distancePoliceMetres?.toString() ?: "",
            avisGlobal = site.avisGlobal,
            travauxAmenagement = site.travauxAmenagement,
            motifDefavorable = site.motifDefavorable,
            observationsParticulieres = site.observationsParticulieres,
            nomSignatureEnqueteur = site.nomSignatureEnqueteur,
            dateEvaluation = site.dateEvaluation,
        )
    }

    // I. Changer de province/ville vide les champs qui en dépendent (listes en cascade).
    fun onProvinceChange(v: String) = update { copy(province = v.uppercase(), ville = "", commune = "") }
    fun onVilleChange(v: String) = update { copy(ville = v.uppercase(), commune = "") }
    fun onCommuneChange(v: String) = update { copy(commune = v.uppercase()) }
    fun onQuartierGroupementChange(v: String) = update { copy(quartierGroupement = v.uppercase()) }
    fun onVillageAvenueRueChange(v: String) = update { copy(villageAvenueRue = v.uppercase()) }
    fun onNomSiteChange(v: String) = update { copy(nomSite = v.uppercase()) }
    fun onNumeroTelephoneChange(v: String) = update { copy(numeroTelephone = v) }
    fun onSerieTelephoneGpsChange(v: String) = update { copy(serieTelephoneGps = v) }

    // III.
    fun onTypeSiteChange(v: String?) = update { copy(typeSite = v) }
    fun onTypeSiteAutreChange(v: String) = update { copy(typeSiteAutre = v.uppercase()) }

    // IV.
    fun onProprieteStatutChange(v: String?) = update { copy(proprieteStatut = v) }
    fun onOptionOccupationChange(v: String?) = update { copy(optionOccupation = v) }
    fun onDisponibiliteImmediateChange(v: Boolean?) = update { copy(disponibiliteImmediate = v) }
    fun onDelaiEstimeChange(v: String) = update { copy(delaiEstime = v) }
    fun onProprietaireNomContactChange(v: String) = update { copy(proprietaireNomContact = v.uppercase()) }

    // V.
    fun onTypeInfrastructureChange(v: String?) = update { copy(typeInfrastructure = v) }
    fun onTypeInfrastructureAutreChange(v: String) = update { copy(typeInfrastructureAutre = v.uppercase()) }
    fun onSuperficieBatimentChange(v: String) = update { copy(superficieBatiment = v.filterDecimal()) }
    fun onSuperficieTerrainChange(v: String) = update { copy(superficieTerrain = v.filterDecimal()) }
    fun onNombrePiecesChange(v: String) = update { copy(nombrePieces = v.filterDigits()) }
    fun onEtatBatimentChange(v: String?) = update { copy(etatBatiment = v) }
    fun onEtatBatimentObservationsChange(v: String) = update { copy(etatBatimentObservations = v.uppercase()) }

    // VI.
    fun onAccesRoutierChange(v: String?) = update { copy(accesRoutier = v) }
    fun onRaccordeSnelChange(v: Boolean?) = update { copy(raccordeSnel = v) }
    fun onStabiliteSnelChange(v: String?) = update { copy(stabiliteSnel = v) }
    fun onGroupeElectrogeneSolaireChange(v: Boolean?) = update { copy(groupeElectrogeneSolaire = v) }
    fun onNonElectrifieChange(v: Boolean?) = update { copy(nonElectrifie = v) }
    fun onRegidesoChange(v: Boolean?) = update { copy(regideso = v) }
    fun onSanitairesChange(v: Boolean?) = update { copy(sanitaires = v) }
    fun onConnectiviteInternetChange(v: String?) = update { copy(connectiviteInternet = v) }
    fun onReseauxDisponiblesChange(v: String) = update { copy(reseauxDisponibles = v.uppercase()) }
    fun onClotureSecuriseChange(v: Boolean?) = update { copy(clotureSecurise = v) }
    fun onProximitePoliceChange(v: Boolean?) = update { copy(proximitePolice = v) }
    fun onDistancePoliceMetresChange(v: String) = update { copy(distancePoliceMetres = v.filterDigits()) }

    // VII.
    fun onAvisGlobalChange(v: String?) = update { copy(avisGlobal = v) }
    fun onTravauxAmenagementChange(v: String) = update { copy(travauxAmenagement = v.uppercase()) }
    fun onMotifDefavorableChange(v: String) = update { copy(motifDefavorable = v.uppercase()) }
    fun onObservationsParticulieresChange(v: String) = update { copy(observationsParticulieres = v.uppercase()) }
    fun onNomSignatureEnqueteurChange(v: String) = update { copy(nomSignatureEnqueteur = v.uppercase()) }
    fun onDateEvaluationChange(v: String) = update { copy(dateEvaluation = formatIsoDateDigits(v)) }

    fun captureLocation() {
        update { copy(locating = true, locationError = null) }
        viewModelScope.launch {
            if (!locationHelper.hasPermission()) {
                update { copy(locating = false, locationError = "Autorisation de localisation refusée.") }
                return@launch
            }
            val fix = locationHelper.bestFix() ?: locationHelper.recentLastKnown()
            if (fix == null) {
                update { copy(locating = false, locationError = "Position indisponible — activez le GPS et réessayez.") }
            } else {
                update {
                    copy(
                        locating = false,
                        locationError = null,
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        altitude = fix.altitude,
                        gpsPrecision = fix.accuracyMeters.toDouble(),
                    )
                }
            }
        }
    }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        update { copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val now = Instant.now().toString()
            val existing = AppContainer.siteStore.get(state.siteId)

            val site = CapturedSite(
                id = state.siteId,
                province = state.province.trim(),
                ville = state.ville.trim(),
                commune = state.commune.trim(),
                quartierGroupement = state.quartierGroupement.trim(),
                villageAvenueRue = state.villageAvenueRue.trim(),
                nomSite = state.nomSite.trim(),
                numeroTelephone = state.numeroTelephone.trim(),
                serieTelephoneGps = state.serieTelephoneGps.trim(),
                latitude = state.latitude,
                longitude = state.longitude,
                altitude = state.altitude,
                gpsPrecision = state.gpsPrecision,
                typeSite = state.typeSite,
                typeSiteAutre = state.typeSiteAutre.trim(),
                proprieteStatut = state.proprieteStatut,
                optionOccupation = state.optionOccupation,
                disponibiliteImmediate = state.disponibiliteImmediate,
                delaiEstime = state.delaiEstime.trim(),
                proprietaireNomContact = state.proprietaireNomContact.trim(),
                typeInfrastructure = state.typeInfrastructure,
                typeInfrastructureAutre = state.typeInfrastructureAutre.trim(),
                superficieBatimentM2 = state.superficieBatiment.toDoubleOrNull(),
                superficieTerrainM2 = state.superficieTerrain.toDoubleOrNull(),
                nombrePieces = state.nombrePieces.toIntOrNull(),
                etatBatiment = state.etatBatiment,
                etatBatimentObservations = state.etatBatimentObservations.trim(),
                accesRoutier = state.accesRoutier,
                raccordeSnel = state.raccordeSnel,
                stabiliteSnel = state.stabiliteSnel,
                groupeElectrogeneSolaire = state.groupeElectrogeneSolaire,
                nonElectrifie = state.nonElectrifie,
                regideso = state.regideso,
                sanitaires = state.sanitaires,
                connectiviteInternet = state.connectiviteInternet,
                reseauxDisponibles = state.reseauxDisponibles.trim(),
                clotureSecurise = state.clotureSecurise,
                proximitePolice = state.proximitePolice,
                distancePoliceMetres = state.distancePoliceMetres.toIntOrNull(),
                avisGlobal = state.avisGlobal,
                travauxAmenagement = state.travauxAmenagement.trim(),
                motifDefavorable = state.motifDefavorable.trim(),
                observationsParticulieres = state.observationsParticulieres.trim(),
                nomSignatureEnqueteur = state.nomSignatureEnqueteur.trim(),
                dateEvaluation = state.dateEvaluation.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncedAt = null,
            )
            AppContainer.siteStore.upsert(site)

            try {
                SiteSyncRepository.sync(site)
            } catch (e: Exception) {
                // Échec réseau ou serveur : la fiche reste enregistrée localement, synchronisable
                // plus tard depuis la liste — ce n'est pas un échec de la saisie elle-même.
            }

            update { copy(saving = false) }
            onDone()
        }
    }

    private inline fun update(block: SiteFormState.() -> SiteFormState) {
        _uiState.value = _uiState.value.block()
    }
}

/** Formate une saisie de chiffres en AAAA-MM-JJ au fil de la frappe (ajoute les "-" tout seul) —
 * même principe que formatDateDigits (CaptureScreen) mais au format ISO attendu par le backend. */
fun formatIsoDateDigits(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    return buildString {
        digits.forEachIndexed { index, c ->
            if (index == 4 || index == 6) append('-')
            append(c)
        }
    }
}

private fun String.filterDigits(): String = filter { it.isDigit() }.take(6)
private fun String.filterDecimal(): String {
    val cleaned = filter { it.isDigit() || it == '.' }
    val firstDot = cleaned.indexOf('.')
    if (firstDot == -1) return cleaned.take(8)
    return (cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "")).take(9)
}
