package com.onip.cartoonip.ui.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.LocationHelper
import com.onip.cartoonip.data.PhotoFiles
import com.onip.cartoonip.data.SyncRepository
import com.onip.cartoonip.data.generateCodeMenage
import com.onip.cartoonip.data.model.CapturedHousehold
import com.onip.cartoonip.data.model.CapturedMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.util.UUID

// Mêmes options que l'app React (types/household.ts RELATION_OPTIONS) — lien du membre avec
// le chef de ménage.
val RELATION_OPTIONS = listOf(
    "Époux/Épouse",
    "Fils/Fille",
    "Père/Mère",
    "Frère/Sœur",
    "Petit-fils/Petite-fille",
    "Beau-fils/Belle-fille",
    "Neveu/Nièce",
    "Autre parent",
    "Employé(e) de maison",
    "Sans lien de parenté",
)

data class MemberInput(
    val key: String = UUID.randomUUID().toString(),
    val nom: String = "",
    val postnom: String = "",
    val prenom: String = "",
    val dateNaissance: String = "",
    val sexe: String? = null,
    val relation: String = "",
)

data class CaptureUiState(
    val householdId: String = UUID.randomUUID().toString(),
    val chefNom: String = "",
    val chefPostnom: String = "",
    val chefPrenom: String = "",
    val chefDateNaissance: String = "",
    val chefSexe: String? = null,
    val ville: String = "",
    val commune: String = "",
    val quartier: String = "",
    val rue: String = "",
    val numero: String = "",
    val immeuble: String = "",
    val members: List<MemberInput> = emptyList(),
    val membersExpanded: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationPrecision: Double? = null,
    val gpsLoading: Boolean = false,
    val gpsError: String? = null,
    val generatedCode: String? = null,
    val photoUri: Uri? = null,
    val photoError: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedCode: String? = null,
    val isEditing: Boolean = false,
) {
    val hasLocation get() = latitude != null && longitude != null
    val chefFilled get() = chefNom.isNotBlank()
    val addressFilled get() = commune.isNotBlank() && quartier.isNotBlank()
    val canGenerateCode get() = chefFilled && addressFilled
    val hasPhoto get() = photoUri != null

    // Le GPS est obligatoire ; la photo de la fiche est recommandée mais pas bloquante (utile si
    // l'appareil photo est indisponible ou pour compléter plus tard).
    val canSubmit get() = canGenerateCode && generatedCode != null && hasLocation
}

class CaptureViewModel : ViewModel() {

    private val locationHelper = LocationHelper(AppContainer.appContext)

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    // Toute saisie est forcée en majuscules — convention de la fiche papier FACM01.
    private fun String.toFieldCase() = uppercase()

    fun onChefNomChange(v: String) = update { copy(chefNom = v.toFieldCase(), generatedCode = null) }
    fun onChefPostnomChange(v: String) = update { copy(chefPostnom = v.toFieldCase(), generatedCode = null) }
    fun onChefPrenomChange(v: String) = update { copy(chefPrenom = v.toFieldCase(), generatedCode = null) }
    fun onChefDateNaissanceChange(v: String) = update { copy(chefDateNaissance = formatDateDigits(v)) }
    fun onChefSexeChange(v: String) = update { copy(chefSexe = v) }
    fun onVilleChange(v: String) = update { copy(ville = v.toFieldCase(), generatedCode = null) }
    fun onCommuneChange(v: String) = update { copy(commune = v.toFieldCase(), generatedCode = null) }
    fun onQuartierChange(v: String) = update { copy(quartier = v.toFieldCase(), generatedCode = null) }
    fun onRueChange(v: String) = update { copy(rue = v.toFieldCase(), generatedCode = null) }
    fun onNumeroChange(v: String) = update { copy(numero = v.toFieldCase(), generatedCode = null) }
    fun onImmeubleChange(v: String) = update { copy(immeuble = v.toFieldCase(), generatedCode = null) }

    fun toggleMembersExpanded() = update { copy(membersExpanded = !membersExpanded) }

    fun addMember() = update { copy(members = members + MemberInput(), membersExpanded = true) }

    fun removeMember(key: String) = update { copy(members = members.filterNot { it.key == key }) }

    fun onMemberNomChange(key: String, v: String) = updateMember(key) { copy(nom = v.toFieldCase()) }
    fun onMemberPostnomChange(key: String, v: String) = updateMember(key) { copy(postnom = v.toFieldCase()) }
    fun onMemberPrenomChange(key: String, v: String) = updateMember(key) { copy(prenom = v.toFieldCase()) }
    fun onMemberDateNaissanceChange(key: String, v: String) = updateMember(key) { copy(dateNaissance = formatDateDigits(v)) }
    fun onMemberSexeChange(key: String, v: String) = updateMember(key) { copy(sexe = v) }
    fun onMemberRelationChange(key: String, v: String) = updateMember(key) { copy(relation = v) }

    private inline fun updateMember(key: String, block: MemberInput.() -> MemberInput) {
        update { copy(members = members.map { if (it.key == key) it.block() else it }) }
    }

    private inline fun update(block: CaptureUiState.() -> CaptureUiState) {
        _uiState.value = _uiState.value.block()
    }

    /** Recharge un ménage déjà enregistré localement pour le corriger. */
    fun loadForEdit(householdId: String) {
        val h = AppContainer.captureStore.get(householdId) ?: return
        _uiState.value = CaptureUiState(
            householdId = h.id,
            chefNom = h.chefNom,
            chefPostnom = h.chefPostnom,
            chefPrenom = h.chefPrenom,
            chefDateNaissance = h.chefDateNaissance,
            chefSexe = h.chefSexe,
            ville = h.ville,
            commune = h.commune,
            quartier = h.quartier,
            rue = h.rue,
            numero = h.numero,
            immeuble = h.immeuble,
            members = h.membres.map {
                MemberInput(
                    nom = it.nom, postnom = it.postnom, prenom = it.prenom,
                    dateNaissance = it.dateNaissance, sexe = it.sexe, relation = it.relation,
                )
            },
            membersExpanded = h.membres.isNotEmpty(),
            latitude = h.latitude,
            longitude = h.longitude,
            locationPrecision = h.locationPrecision,
            generatedCode = h.codeMenage,
            photoUri = h.photoPath?.let { PhotoFiles.uriFor(AppContainer.appContext, java.io.File(it)) },
            isEditing = true,
        )
    }

    fun captureLocation() {
        update { copy(gpsLoading = true, gpsError = null) }
        viewModelScope.launch {
            if (!locationHelper.hasPermission()) {
                update { copy(gpsLoading = false, gpsError = "Autorisation de localisation refusée.") }
                return@launch
            }
            val fix = withTimeoutOrNull(20_000) { locationHelper.awaitFix() } ?: locationHelper.lastKnown()
            if (fix == null) {
                update { copy(gpsLoading = false, gpsError = "Position indisponible — activez le GPS et réessayez.") }
            } else {
                update {
                    copy(
                        gpsLoading = false,
                        gpsError = null,
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        locationPrecision = fix.accuracyMeters.toDouble(),
                    )
                }
            }
        }
    }

    fun generateCode() {
        if (!_uiState.value.canGenerateCode) return
        update { copy(generatedCode = generateCodeMenage()) }
    }

    fun photoFileUri(): Uri {
        val file = PhotoFiles.fileFor(AppContainer.appContext, _uiState.value.householdId)
        return PhotoFiles.uriFor(AppContainer.appContext, file)
    }

    fun onPhotoCaptured(success: Boolean) {
        if (success) {
            // Le fichier existe-t-il vraiment et fait-il un poids plausible ? Certains appareils
            // renvoient "success" alors que le fichier est resté vide (annulation silencieuse).
            val file = PhotoFiles.fileFor(AppContainer.appContext, _uiState.value.householdId)
            if (file.exists() && file.length() > 0) {
                update { copy(photoUri = photoFileUri(), photoError = null) }
            } else {
                update { copy(photoUri = null, photoError = "La photo n'a pas été enregistrée — réessayez.") }
            }
        } else {
            update { copy(photoError = "Prise de photo annulée.") }
        }
    }

    fun submit(onDone: (String) -> Unit) {
        val state = _uiState.value
        if (!state.canSubmit) return

        update { copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val now = Instant.now().toString()
            val photoPath = if (state.hasPhoto) {
                PhotoFiles.fileFor(AppContainer.appContext, state.householdId).absolutePath
            } else null
            // En modification, on garde la date de création d'origine et on force une
            // re-synchronisation (champs + photo) puisque le contenu a pu changer.
            val existing = AppContainer.captureStore.get(state.householdId)

            var household = CapturedHousehold(
                id = state.householdId,
                codeMenage = state.generatedCode!!,
                chefNom = state.chefNom.trim(),
                chefPostnom = state.chefPostnom.trim(),
                chefPrenom = state.chefPrenom.trim(),
                chefDateNaissance = state.chefDateNaissance.trim(),
                chefSexe = state.chefSexe,
                ville = state.ville.trim(),
                commune = state.commune.trim(),
                quartier = state.quartier.trim(),
                rue = state.rue.trim(),
                numero = state.numero.trim(),
                immeuble = state.immeuble.trim(),
                membres = state.members
                    .filter { it.nom.isNotBlank() || it.postnom.isNotBlank() || it.prenom.isNotBlank() }
                    .map {
                        CapturedMember(
                            nom = it.nom.trim(),
                            postnom = it.postnom.trim(),
                            prenom = it.prenom.trim(),
                            dateNaissance = it.dateNaissance.trim(),
                            sexe = it.sexe,
                            relation = it.relation,
                        )
                    },
                latitude = state.latitude,
                longitude = state.longitude,
                locationPrecision = state.locationPrecision,
                photoPath = photoPath,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncedAt = null,
                photoSyncedAt = null,
            )
            AppContainer.captureStore.upsert(household)

            try {
                household = SyncRepository.sync(household)
            } catch (e: Exception) {
                // Échec réseau ou serveur : le ménage reste enregistré localement, synchronisable
                // plus tard depuis le Journal — ce n'est pas un échec de la capture elle-même.
            }

            update { copy(isSaving = false, savedCode = household.codeMenage) }
            onDone(household.codeMenage)
        }
    }

    fun resetForm() {
        _uiState.value = CaptureUiState()
    }
}

/** Formate une saisie de chiffres en JJ/MM/AAAA au fil de la frappe (ajoute les "/" tout seul). */
fun formatDateDigits(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    return buildString {
        digits.forEachIndexed { index, c ->
            if (index == 2 || index == 4) append('/')
            append(c)
        }
    }
}
