package com.onip.cartoonip.data

import android.content.Context
import com.google.gson.Gson
import com.onip.cartoonip.data.model.CapturedSite

/**
 * Questionnaire terrain de la fiche de repérage (« Questionnaire Terrain Bureaux État Civil
 * ONIP »), lu depuis assets/questionnaire-site.json — le même fichier que le backend
 * (resources/questionnaire/questionnaire-site.json, cf. SiteQuestionnaire.java).
 *
 * Chaque réponse a une clé : l'id de la question (ex. "q8"), "id_ligne" pour un tableau (ex.
 * "q21_portes"), et la clé de précision d'un choix « Autre (à préciser) ». Les questions "bound"
 * correspondent à un champ existant de [CapturedSite] (ex. typeSite pour Q1), les autres partent
 * dans [CapturedSite.questionnaire]. Choix multiples "A|B", oui/non "OUI"/"NON".
 */
data class QSection(
    val id: String = "",
    val title: String = "",
    val hideIf: Map<String, List<String>>? = null,
    val items: List<QItem> = emptyList(),
)

data class QItem(
    val id: String = "",
    val num: String? = null,
    val label: String = "",
    val type: String = "text",
    val options: List<List<String>>? = null,
    val legacyOptions: List<List<String>>? = null,
    val precise: List<String>? = null,
    val precisionId: String? = null,
    val bound: Boolean = false,
    val showIf: Map<String, List<String>>? = null,
    val hideIf: Map<String, List<String>>? = null,
    val columns: List<List<String>>? = null,
    val rows: List<List<String>>? = null,
) {
    val displayLabel: String get() = if (num.isNullOrBlank()) label else "$num. $label"
    val isNote: Boolean get() = type == "note"
    val hasPrecision: Boolean get() = !precise.isNullOrEmpty()
    val precisionKey: String get() = precisionId ?: "${id}_precision"
    fun rowKey(row: String) = "${id}_$row"
    val choices: List<Pair<String, String>> get() = options.orEmpty().map { it[0] to it[1] }

    /** Clés de réponse de cette question (vide pour une simple note). */
    val keys: List<String>
        get() = when {
            isNote -> emptyList()
            type == "grid" -> rows.orEmpty().map { rowKey(it[0]) }
            hasPrecision -> listOf(id, precisionKey)
            else -> listOf(id)
        }
}

private data class QDocument(val sections: List<QSection> = emptyList())

object SiteQuestionnaire {

    @Volatile
    private var cached: List<QSection>? = null

    fun sections(context: Context): List<QSection> = cached ?: synchronized(this) {
        cached ?: context.assets.open("questionnaire-site.json").bufferedReader(Charsets.UTF_8).use {
            Gson().fromJson(it, QDocument::class.java).sections
        }.also { cached = it }
    }

    /** Clés des réponses sans champ dédié (celles rangées dans CapturedSite.questionnaire). */
    fun jsonKeys(context: Context): Set<String> =
        sections(context).flatMap { s -> s.items.filter { !it.bound }.flatMap { it.keys } }.toSet()

    // Sauts du questionnaire : showIf = toutes les conditions remplies, hideIf = l'une d'elles ;
    // réponse de référence vide = rien à sauter (mêmes règles que le backend).
    fun visible(item: QItem, answers: Map<String, String>): Boolean =
        !hiddenBy(item.hideIf, answers) &&
            item.showIf.orEmpty().all { (key, values) -> matches(answers[key], values) }

    fun hiddenBy(hideIf: Map<String, List<String>>?, answers: Map<String, String>): Boolean =
        hideIf.orEmpty().any { (key, values) -> matches(answers[key], values) }

    private fun matches(answer: String?, values: List<String>): Boolean =
        !answer.isNullOrBlank() && answer.split('|').any { it in values }

    // ------------------------------------------------------------------ champs dédiés

    /** Réponses des questions reliées à un champ existant de la fiche. */
    fun boundAnswers(site: CapturedSite): Map<String, String> = buildMap {
        // Pas « put » : ce nom masquerait MutableMap.put et s'appellerait lui-même à l'infini.
        fun add(key: String, value: String?) { if (!value.isNullOrBlank()) this[key] = value }
        fun yn(b: Boolean?) = b?.let { if (it) "OUI" else "NON" }
        fun num(d: Double?) = d?.let { if (it == Math.rint(it)) it.toLong().toString() else it.toString() }
        add("typeSite", site.typeSite)
        add("typeSiteAutre", site.typeSiteAutre)
        add("optionOccupation", site.optionOccupation)
        add("disponibiliteImmediate", yn(site.disponibiliteImmediate))
        add("delaiEstime", site.delaiEstime)
        add("proprietaireNomContact", site.proprietaireNomContact)
        add("proprieteStatut", site.proprieteStatut)
        add("dateEvaluation", site.dateEvaluation)
        add("nomSignatureEnqueteur", site.nomSignatureEnqueteur)
        add("etatBatiment", site.etatBatiment)
        add("superficieTerrainM2", num(site.superficieTerrainM2))
        add("superficieBatimentM2", num(site.superficieBatimentM2))
        add("regideso", yn(site.regideso))
        add("sanitaires", yn(site.sanitaires))
        add("nombrePieces", site.nombrePieces?.toString())
        add("connectiviteInternet", site.connectiviteInternet)
        add("proximitePolice", yn(site.proximitePolice))
        add("observationsParticulieres", site.observationsParticulieres)
        add("typeInfrastructure", site.typeInfrastructure)
        add("typeInfrastructureAutre", site.typeInfrastructureAutre)
        add("etatBatimentObservations", site.etatBatimentObservations)
        add("accesRoutier", site.accesRoutier)
        add("raccordeSnel", yn(site.raccordeSnel))
        add("stabiliteSnel", site.stabiliteSnel)
        add("groupeElectrogeneSolaire", yn(site.groupeElectrogeneSolaire))
        add("nonElectrifie", yn(site.nonElectrifie))
        add("reseauxDisponibles", site.reseauxDisponibles)
        add("clotureSecurise", yn(site.clotureSecurise))
        add("distancePoliceMetres", site.distancePoliceMetres?.toString())
        add("avisGlobal", site.avisGlobal)
        add("travauxAmenagement", site.travauxAmenagement)
        add("motifDefavorable", site.motifDefavorable)
    }

    /** Recopie les réponses des questions reliées dans les champs dédiés de la fiche. */
    fun applyBound(site: CapturedSite, a: Map<String, String>): CapturedSite {
        fun s(key: String) = a[key]?.trim().orEmpty()
        fun opt(key: String) = a[key]?.trim()?.ifBlank { null }
        fun b(key: String) = when (a[key]) { "OUI" -> true; "NON" -> false; else -> null }
        fun d(key: String) = a[key]?.replace(',', '.')?.toDoubleOrNull()
        fun i(key: String) = a[key]?.toIntOrNull()
        return site.copy(
            typeSite = opt("typeSite"),
            typeSiteAutre = s("typeSiteAutre"),
            optionOccupation = opt("optionOccupation"),
            disponibiliteImmediate = b("disponibiliteImmediate"),
            delaiEstime = s("delaiEstime"),
            proprietaireNomContact = s("proprietaireNomContact"),
            proprieteStatut = opt("proprieteStatut"),
            dateEvaluation = s("dateEvaluation"),
            nomSignatureEnqueteur = s("nomSignatureEnqueteur"),
            etatBatiment = opt("etatBatiment"),
            superficieTerrainM2 = d("superficieTerrainM2"),
            superficieBatimentM2 = d("superficieBatimentM2"),
            regideso = b("regideso"),
            sanitaires = b("sanitaires"),
            nombrePieces = i("nombrePieces"),
            connectiviteInternet = opt("connectiviteInternet"),
            proximitePolice = b("proximitePolice"),
            observationsParticulieres = s("observationsParticulieres"),
            typeInfrastructure = opt("typeInfrastructure"),
            typeInfrastructureAutre = s("typeInfrastructureAutre"),
            etatBatimentObservations = s("etatBatimentObservations"),
            accesRoutier = opt("accesRoutier"),
            raccordeSnel = b("raccordeSnel"),
            stabiliteSnel = opt("stabiliteSnel"),
            groupeElectrogeneSolaire = b("groupeElectrogeneSolaire"),
            nonElectrifie = b("nonElectrifie"),
            reseauxDisponibles = s("reseauxDisponibles"),
            clotureSecurise = b("clotureSecurise"),
            distancePoliceMetres = i("distancePoliceMetres"),
            avisGlobal = opt("avisGlobal"),
            travauxAmenagement = s("travauxAmenagement"),
            motifDefavorable = s("motifDefavorable"),
        )
    }
}
