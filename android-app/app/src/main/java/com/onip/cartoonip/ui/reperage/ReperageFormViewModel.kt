package com.onip.cartoonip.ui.reperage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.LocationHelper
import com.onip.cartoonip.data.QSection
import com.onip.cartoonip.data.SiteQuestionnaire
import com.onip.cartoonip.data.model.CapturedSite
import com.onip.cartoonip.data.model.ZoneSiteOption
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * État du formulaire de fiche de repérage de site : les champs d'identification (Q7 a à f, j,
 * série GPS) et le GPS restent des champs dédiés ; toutes les autres questions du questionnaire
 * terrain (Module 0 à 5 + rubriques de l'ancienne fiche) sont dans [answers], indexées par leur
 * clé (cf. SiteQuestionnaire). Rien n'est obligatoire : [save] accepte la fiche à tout stade de
 * remplissage, comme le reste de l'app terrain tolère un brouillon synchronisé puis complété.
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

    // Q7. Identité du site / bureau
    val province: String = "",
    val ville: String = "",
    val commune: String = "",
    val quartierGroupement: String = "",
    val villageAvenueRue: String = "",
    val nomSite: String = "",
    val numeroTelephone: String = "",
    val serieTelephoneGps: String = "",

    // Questionnaire terrain : clé -> réponse ("A|B" pour un choix multiple, "OUI"/"NON").
    val answers: Map<String, String> = emptyMap(),

    val saving: Boolean = false,
    val saveError: String? = null,
)

class ReperageFormViewModel : ViewModel() {

    private val locationHelper = LocationHelper(AppContainer.appContext)

    private val _uiState = MutableStateFlow(SiteFormState())
    val uiState: StateFlow<SiteFormState> = _uiState.asStateFlow()

    val sections: List<QSection> = SiteQuestionnaire.sections(AppContainer.appContext)
    private val jsonKeys: Set<String> = SiteQuestionnaire.jsonKeys(AppContainer.appContext)

    // Chef d'équipe : le nom du site se choisit uniquement parmi les sites de sa zone (importés
    // depuis le tableau de bord), et ce choix remplit province, ville/territoire, commune et
    // adresse. Les autres rôles gardent la saisie libre.
    val restrictToZoneSites: Boolean = AppContainer.agentRole.value == "CHEF_EQUIPE"
    val zoneSites: StateFlow<List<ZoneSiteOption>> = AppContainer.zoneSites

    // Communes de la zone : seul choix proposé pour la commune quand la fiche est restreinte.
    val zoneCommunes: List<String> = AppContainer.agentZone.value?.communes.orEmpty()

    init {
        if (restrictToZoneSites) {
            viewModelScope.launch { AppContainer.refreshZoneSites() }
        }
    }

    fun preselectZoneSite(zoneSiteId: String) {
        AppContainer.zoneSites.value.firstOrNull { it.id == zoneSiteId }?.let(::onZoneSiteSelected)
    }

    fun onZoneSiteSelected(option: ZoneSiteOption) = update {
        copy(
            nomSite = option.nomSite.uppercase(),
            province = option.province?.uppercase() ?: province,
            ville = option.villeTerritoire?.uppercase() ?: ville,
            commune = option.commune?.uppercase().orEmpty(),
            quartierGroupement = option.quartier?.uppercase() ?: quartierGroupement,
            villageAvenueRue = option.adresse?.uppercase() ?: villageAvenueRue,
        )
    }

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
            answers = site.questionnaire.orEmpty() + SiteQuestionnaire.boundAnswers(site),
        )
    }

    // Changer de province/ville vide les champs qui en dépendent (listes en cascade).
    fun onProvinceChange(v: String) = update { copy(province = v.uppercase(), ville = "", commune = "", quartierGroupement = "") }
    fun onVilleChange(v: String) = update { copy(ville = v.uppercase(), commune = "", quartierGroupement = "") }
    fun onCommuneChange(v: String) = update {
        copy(commune = v.uppercase(), quartierGroupement = if (v.equals(commune, ignoreCase = true)) quartierGroupement else "")
    }
    fun onQuartierGroupementChange(v: String) = update { copy(quartierGroupement = v.uppercase()) }
    fun onVillageAvenueRueChange(v: String) = update { copy(villageAvenueRue = v.uppercase()) }
    fun onNomSiteChange(v: String) = update { copy(nomSite = v.uppercase()) }
    fun onNumeroTelephoneChange(v: String) = update { copy(numeroTelephone = v) }
    fun onSerieTelephoneGpsChange(v: String) = update { copy(serieTelephoneGps = v) }

    /** Réponse à une question du questionnaire ("" efface la réponse). */
    fun onAnswer(key: String, value: String) = update {
        copy(answers = if (value.isBlank()) answers - key else answers + (key to value))
    }

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

            val base = CapturedSite(
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
                questionnaire = state.answers
                    .filterKeys { it in jsonKeys }
                    .mapValues { it.value.trim() }
                    .filterValues { it.isNotEmpty() },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncedAt = null,
            )
            // Enregistrée sur l'appareil, en attente : l'envoi au serveur se fait depuis l'accueil
            // ou la liste, après saisie du mot de passe (cf. SecureSync).
            AppContainer.siteStore.upsert(SiteQuestionnaire.applyBound(base, state.answers))

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

internal fun String.filterDigits(): String = filter { it.isDigit() }.take(9)
internal fun String.filterDecimal(): String {
    val cleaned = replace(',', '.').filter { it.isDigit() || it == '.' }
    val firstDot = cleaned.indexOf('.')
    if (firstDot == -1) return cleaned.take(9)
    return (cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "")).take(12)
}
