package com.onip.cartoonip.ui.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.LocationHelper
import com.onip.cartoonip.data.PhotoFiles
import com.onip.cartoonip.data.SyncRepository
import com.onip.cartoonip.data.generateCodeMenage
import com.onip.cartoonip.data.provinceForVille
import com.onip.cartoonip.data.model.CapturedHousehold
import com.onip.cartoonip.data.model.CapturedMember
import com.onip.cartoonip.data.model.MAX_HOUSEHOLD_PHOTOS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
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
) {
    fun isEmpty() = nom.isBlank() && postnom.isBlank() && prenom.isBlank() && dateNaissance.isBlank() &&
        sexe == null && relation.isBlank()
}

// Chef + 14 membres au maximum, comme la fiche papier (même limite que l'app web).
const val MAX_TOTAL_MEMBERS = 15

data class CaptureUiState(
    val householdId: String = UUID.randomUUID().toString(),
    // Nombre de membres déclaré (chef compris), saisi au-dessus du chef de ménage ; les fiches
    // membres s'ajustent à ce nombre (cf. onDeclaredMembersChange).
    val declaredMembersText: String = "1",
    val chefNom: String = "",
    val chefPostnom: String = "",
    val chefPrenom: String = "",
    val chefDateNaissance: String = "",
    val chefSexe: String? = null,
    val province: String = "",
    val ville: String = "",
    val commune: String = "",
    val quartier: String = "",
    val rue: String = "",
    val numero: String = "",
    val immeuble: String = "",
    val etage: String = "",
    val members: List<MemberInput> = emptyList(),
    val membersExpanded: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationPrecision: Double? = null,
    val gpsLoading: Boolean = false,
    val gpsError: String? = null,
    val generatedCode: String? = null,
    val photoPaths: List<String> = emptyList(),
    val photoError: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedCode: String? = null,
    val isEditing: Boolean = false,
) {
    val hasLocation get() = latitude != null && longitude != null
    val chefFilled get() = chefNom.isNotBlank()
    val addressFilled get() = commune.isNotBlank() && quartier.isNotBlank()
    // Le code étant dérivé du GPS + chef + adresse (cf. CodeGenerator), le GPS est désormais une
    // condition de génération, pas seulement de soumission.
    val canGenerateCode get() = hasLocation && chefFilled && addressFilled
    val hasPhoto get() = photoPaths.isNotEmpty()
    val canAddPhoto get() = photoPaths.size < MAX_HOUSEHOLD_PHOTOS

    // Chef + membres additionnels — recalculé automatiquement à chaque ajout/retrait de fiche
    // membre, jamais saisi à la main (cf. demande "synchronisé avec l'ajout de fiches").
    val totalMembers get() = 1 + members.size

    val declaredMembers: Int? get() = declaredMembersText.toIntOrNull()?.takeIf { it in 1..MAX_TOTAL_MEMBERS }

    // Le nombre déclaré doit correspondre aux fiches, et chaque fiche membre doit avoir un nom.
    val membersConsistent: Boolean
        get() = declaredMembers == totalMembers && members.all { it.nom.isNotBlank() }

    // Le GPS est obligatoire ; la photo de la fiche est recommandée mais pas bloquante (utile si
    // l'appareil photo est indisponible ou pour compléter plus tard).
    val canSubmit get() = canGenerateCode && generatedCode != null && hasLocation && membersConsistent

    // Ce qui bloque encore l'enregistrement, pour l'indiquer à l'agent sous le bouton grisé.
    val missingForSubmit: List<String>
        get() = buildList {
            if (!hasLocation) add("le GPS")
            if (chefNom.isBlank()) add("le nom du chef de ménage")
            if (commune.isBlank()) add("la commune")
            if (quartier.isBlank()) add("le quartier")
            val declared = declaredMembers
            when {
                declared == null -> add("le nombre de membres (1 à $MAX_TOTAL_MEMBERS)")
                declared != totalMembers -> add("autant de fiches membres que le nombre déclaré ($declared)")
                members.any { it.nom.isBlank() } -> add("le nom de chaque membre")
            }
        }
}

class CaptureViewModel : ViewModel() {

    private val locationHelper = LocationHelper(AppContainer.appContext)

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    init {
        // Recharge la photo de profil pour l'avatar de la barre du haut — pas persistée en
        // session, donc absente tant qu'on ne l'a pas redemandée après un redémarrage de l'app.
        if (AppContainer.agentPhoto.value == null) {
            viewModelScope.launch {
                runCatching { AppContainer.api().agent.me() }.getOrNull()?.let {
                    AppContainer.setAgentPhoto(it.photoDataUrl)
                }
            }
        }
    }

    // Toute saisie est forcée en majuscules — convention de la fiche papier FACM01.
    private fun String.toFieldCase() = uppercase()

    // Une fois généré, le code du ménage n'est plus jamais invalidé par la suite (même si le
    // chef/l'adresse changent ensuite) — il ne doit pouvoir être régénéré qu'une seule fois.
    fun onChefNomChange(v: String) = update { copy(chefNom = v.toFieldCase()) }
    fun onChefPostnomChange(v: String) = update { copy(chefPostnom = v.toFieldCase()) }
    fun onChefPrenomChange(v: String) = update { copy(chefPrenom = v.toFieldCase()) }
    fun onChefDateNaissanceChange(v: String) = update { copy(chefDateNaissance = formatDateDigits(v)) }
    fun onChefSexeChange(v: String) = update { copy(chefSexe = v) }
    // Changer de province vide la ville et la commune (elles appartenaient à l'ancienne province).
    fun onProvinceChange(v: String) = update { copy(province = v.toFieldCase(), ville = "", commune = "") }

    // Une ville de la liste impose sa province ; une ville saisie librement garde la province
    // choisie à la main.
    fun onVilleChange(v: String) = update {
        copy(
            ville = v.toFieldCase(),
            commune = "",
            province = provinceForVille(v)?.toFieldCase() ?: province,
        )
    }
    fun onCommuneChange(v: String) = update { copy(commune = v.toFieldCase()) }
    fun onQuartierChange(v: String) = update { copy(quartier = v.toFieldCase()) }
    fun onRueChange(v: String) = update { copy(rue = v.toFieldCase()) }
    fun onNumeroChange(v: String) = update { copy(numero = v.toFieldCase()) }
    fun onImmeubleChange(v: String) = update { copy(immeuble = v.toFieldCase()) }
    fun onEtageChange(v: String) = update { copy(etage = v.toFieldCase()) }

    fun toggleMembersExpanded() = update { copy(membersExpanded = !membersExpanded) }

    // Ajouter/retirer une fiche met à jour le nombre déclaré, et inversement.
    fun addMember() = update {
        if (totalMembers >= MAX_TOTAL_MEMBERS) this
        else copy(members = members + MemberInput(), membersExpanded = true, declaredMembersText = (totalMembers + 1).toString())
    }

    // Saisir un nombre ajoute les fiches manquantes, ou retire les fiches vides en trop (une fiche
    // déjà renseignée n'est jamais supprimée d'office : l'enregistrement reste alors bloqué tant
    // que l'agent ne l'a pas retirée lui-même).
    fun onDeclaredMembersChange(v: String) = update {
        val text = v.filter { it.isDigit() }.take(2)
        val wanted = text.toIntOrNull()?.takeIf { it in 1..MAX_TOTAL_MEMBERS }
            ?: return@update copy(declaredMembersText = text)
        var adjusted = members
        while (adjusted.size < wanted - 1) adjusted = adjusted + MemberInput()
        while (adjusted.size > wanted - 1 && adjusted.last().isEmpty()) adjusted = adjusted.dropLast(1)
        copy(declaredMembersText = text, members = adjusted, membersExpanded = membersExpanded || adjusted.isNotEmpty())
    }

    fun removeMember(key: String) = update {
        val remaining = members.filterNot { it.key == key }
        copy(members = remaining, declaredMembersText = (1 + remaining.size).toString())
    }

    fun onMemberNomChange(key: String, v: String) = updateMember(key) { copy(nom = v.toFieldCase()) }
    fun onMemberPostnomChange(key: String, v: String) = updateMember(key) { copy(postnom = v.toFieldCase()) }
    fun onMemberPrenomChange(key: String, v: String) = updateMember(key) { copy(prenom = v.toFieldCase()) }
    fun onMemberDateNaissanceChange(key: String, v: String) = updateMember(key) { copy(dateNaissance = formatDateDigits(v)) }
    fun onMemberSexeChange(key: String, v: String) = updateMember(key) { copy(sexe = v) }
    fun onMemberRelationChange(key: String, v: String) = updateMember(key) { copy(relation = v) }

    private inline fun updateMember(key: String, block: MemberInput.() -> MemberInput) {
        update { copy(members = members.map { if (it.key == key) it.block() else it }) }
    }

    // Génère le code automatiquement dès que le GPS, le chef et l'adresse sont renseignés — plus
    // besoin d'un bouton "Générer le code" pour ça. Une fois posé, il n'est plus jamais régénéré
    // (même si le chef/l'adresse changent ensuite), quel que soit le champ qui a déclenché cette
    // mise à jour : la condition `generatedCode == null` protège ça.
    private inline fun update(block: CaptureUiState.() -> CaptureUiState) {
        var newState = _uiState.value.block()
        if (newState.canGenerateCode && newState.generatedCode == null) {
            newState = newState.copy(
                generatedCode = generateCodeMenage(
                    latitude = newState.latitude!!,
                    longitude = newState.longitude!!,
                    chefNom = newState.chefNom,
                    commune = newState.commune,
                    quartier = newState.quartier,
                ),
            )
        }
        _uiState.value = newState
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
            province = h.province,
            ville = h.ville,
            commune = h.commune,
            quartier = h.quartier,
            rue = h.rue,
            numero = h.numero,
            immeuble = h.immeuble,
            etage = h.etage,
            declaredMembersText = (1 + h.membres.size).toString(),
            members = h.membres.map {
                MemberInput(
                    nom = it.nom, postnom = it.postnom, prenom = it.prenom,
                    dateNaissance = it.dateNaissance,
                    sexe = it.sexe, relation = it.relation,
                )
            },
            membersExpanded = h.membres.isNotEmpty(),
            latitude = h.latitude,
            longitude = h.longitude,
            locationPrecision = h.locationPrecision,
            generatedCode = h.codeMenage,
            photoPaths = h.photoPaths,
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

    // Emplacement de la photo en cours de prise — un seul bouton "Prendre une photo" réutilisé
    // pour toute la galerie (jusqu'à MAX_HOUSEHOLD_PHOTOS) plutôt qu'un bouton par emplacement ;
    // un jeton unique par prise (pas un simple index 0..3) pour que retirer une photo du milieu
    // de la galerie ne fasse jamais correspondre le mauvais fichier à la mauvaise position.
    private var pendingPhotoFile: File? = null

    fun photoFileUri(): Uri {
        val token = UUID.randomUUID().toString()
        val file = PhotoFiles.fileFor(AppContainer.appContext, _uiState.value.householdId, token)
        pendingPhotoFile = file
        return PhotoFiles.uriFor(AppContainer.appContext, file)
    }

    fun onPhotoCaptured(success: Boolean) {
        val file = pendingPhotoFile
        pendingPhotoFile = null
        // Le fichier existe-t-il vraiment et fait-il un poids plausible ? Certains appareils
        // renvoient "success" alors que le fichier est resté vide (annulation silencieuse).
        if (success && file != null && file.exists() && file.length() > 0) {
            update { copy(photoPaths = (photoPaths + file.absolutePath).take(MAX_HOUSEHOLD_PHOTOS), photoError = null) }
        } else if (success) {
            update { copy(photoError = "La photo n'a pas été enregistrée — réessayez.") }
        } else {
            update { copy(photoError = "Prise de photo annulée.") }
        }
    }

    fun removePhoto(path: String) = update { copy(photoPaths = photoPaths.filterNot { it == path }) }

    fun submit(onDone: (String) -> Unit) {
        val state = _uiState.value
        if (!state.canSubmit) return

        update { copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val now = Instant.now().toString()
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
                province = state.province.trim(),
                ville = state.ville.trim(),
                commune = state.commune.trim(),
                quartier = state.quartier.trim(),
                rue = state.rue.trim(),
                numero = state.numero.trim(),
                immeuble = state.immeuble.trim(),
                etage = state.etage.trim(),
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
                photoPaths = state.photoPaths,
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
