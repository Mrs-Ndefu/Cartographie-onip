package com.onip.cartoonip.data

import android.content.Context
import org.json.JSONObject

// Référentiel géographique de la RDC : provinces -> villes/territoires -> communes/cités/secteurs
// -> quartiers. Même fichier que le backend (backend/src/main/resources/geo/drc-geo.json), généré
// à partir de « Subdivision ADM-RDC » (Carto-Begins) fusionné avec les anciennes listes ; embarqué
// dans assets/ pour fonctionner hors connexion. Non exhaustif : les champs gardent l'option
// « Autre » en secours. Chargé une fois au démarrage (AppContainer.init -> DrcGeo.load).
data class VilleEntry(
    val name: String,
    val province: String,
    val communes: List<String>,
    // commune -> quartiers (vide si non renseignés)
    val quartiers: Map<String, List<String>> = emptyMap(),
)

const val AUTRE_VILLE = "AUTRE"

object DrcGeo {
    @Volatile
    var villes: List<VilleEntry> = emptyList()
        private set

    fun load(context: Context) {
        if (villes.isNotEmpty()) return
        val json = context.assets.open("drc-geo.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(json)
        val array = root.getJSONArray("villes")
        val result = ArrayList<VilleEntry>(array.length())
        for (i in 0 until array.length()) {
            val v = array.getJSONObject(i)
            val communesJson = v.getJSONArray("communes")
            val communes = ArrayList<String>(communesJson.length())
            val quartiers = LinkedHashMap<String, List<String>>()
            for (j in 0 until communesJson.length()) {
                val c = communesJson.getJSONObject(j)
                val name = c.getString("name")
                communes += name
                val qs = c.optJSONArray("quartiers")
                if (qs != null && qs.length() > 0) {
                    quartiers[name] = List(qs.length()) { k -> qs.getString(k) }
                }
            }
            result += VilleEntry(v.getString("name"), v.getString("province"), communes, quartiers)
        }
        villes = result.sortedBy { it.name.lowercase() }
    }
}

val DRC_VILLES: List<VilleEntry>
    get() = DrcGeo.villes

/** Quartiers d'une commune de la liste (vide si inconnue ou non renseignés). */
fun quartiersFor(ville: String, commune: String): List<String> {
    if (ville.isBlank() || commune.isBlank()) return emptyList()
    val entry = DRC_VILLES.find { it.name.equals(ville, ignoreCase = true) } ?: return emptyList()
    return entry.quartiers.entries.firstOrNull { it.key.equals(commune, ignoreCase = true) }?.value ?: emptyList()
}

fun communesForVille(ville: String): List<String> =
    DRC_VILLES.find { it.name.equals(ville, ignoreCase = true) }?.communes ?: emptyList()

// Les 26 provinces de la RDC — champ "Province" du formulaire, placé avant la ville.
val DRC_PROVINCES: List<String> = listOf(
    "Bas-Uele", "Équateur", "Haut-Katanga", "Haut-Lomami", "Haut-Uele", "Ituri", "Kasaï",
    "Kasaï-Central", "Kasaï-Oriental", "Kinshasa", "Kongo-Central", "Kwango", "Kwilu", "Lomami",
    "Lualaba", "Mai-Ndombe", "Maniema", "Mongala", "Nord-Kivu", "Nord-Ubangi", "Sankuru",
    "Sud-Kivu", "Sud-Ubangi", "Tanganyika", "Tshopo", "Tshuapa",
)

/** Province d'une ville de la liste, ou null pour une ville saisie librement. */
fun provinceForVille(ville: String): String? =
    DRC_VILLES.find { it.name.equals(ville, ignoreCase = true) }?.province

/** Villes proposées pour une province (toutes si aucune province n'est choisie). */
fun villesForProvince(province: String): List<VilleEntry> =
    if (province.isBlank()) DRC_VILLES
    else DRC_VILLES.filter { it.province.equals(province, ignoreCase = true) }

// Numéro de la province sur 2 chiffres (01 = Kinshasa, puis les 25 autres par ordre
// alphabétique) — même convention que backend/.../DrcProvinces.numberOf (répliquée ici car le
// code ménage se génère hors ligne, sur le téléphone, sans appel au serveur). Utilisé pour le
// préfixe du code ménage "VC00-XXXXXX" (cf. CodeGenerator.generateCodeMenage).
private val DRC_PROVINCES_BY_LETTER: List<String> =
    listOf("Kinshasa") + DRC_PROVINCES.filter { !it.equals("Kinshasa", ignoreCase = true) }

fun provinceCode(province: String): String {
    val index = DRC_PROVINCES_BY_LETTER.indexOfFirst { it.equals(province, ignoreCase = true) }
    return if (index < 0) "00" else String.format("%02d", index + 1)
}
