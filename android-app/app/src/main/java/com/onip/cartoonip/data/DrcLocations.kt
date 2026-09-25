package com.onip.cartoonip.data

// Villes de la RDC et leurs communes officielles — champs "Ville"/"Commune" du formulaire papier.
// Même source et mêmes limites que la liste équivalente côté web (src/data/drcLocations.ts) :
// non garantie exhaustive à 100%, d'où l'option "Autre" en secours dans les deux champs.
data class VilleEntry(val name: String, val province: String, val communes: List<String>)

const val AUTRE_VILLE = "AUTRE"

val DRC_VILLES: List<VilleEntry> = listOf(
    VilleEntry("Kinshasa", "Kinshasa", listOf(
        "Bandalungwa", "Barumbu", "Bumbu", "Gombe", "Kalamu", "Kasa-Vubu", "Kimbanseke", "Kinshasa",
        "Kintambo", "Kisenso", "Lemba", "Limete", "Lingwala", "Makala", "Maluku", "Masina", "Matete",
        "Mont-Ngafula", "Ndjili", "Ngaba", "Ngaliema", "Ngiri-Ngiri", "Nsele", "Selembao",
    )),
    VilleEntry("Buta", "Bas-Uele", listOf("Babade", "Dobea", "Finant", "Tepatondele")),
    VilleEntry("Aketi", "Bas-Uele", emptyList()),
    VilleEntry("Dingila", "Bas-Uele", emptyList()),
    VilleEntry("Mbandaka", "Équateur", listOf("Mbandaka", "Wangata")),
    VilleEntry("Likasi", "Haut-Katanga", listOf("Kikula", "Likasi", "Panda", "Shituru")),
    VilleEntry("Lubumbashi", "Haut-Katanga", listOf(
        "Annexe", "Kamalondo", "Kampemba", "Katuba", "Kenya", "Lubumbashi", "Ruashi",
    )),
    VilleEntry("Kipushi", "Haut-Katanga", emptyList()),
    VilleEntry("Kamina", "Haut-Lomami", listOf("Dimayi", "Kamina", "Sobongo", "Kupa")),
    VilleEntry("Isiro", "Haut-Uele", listOf("Mambaya", "Mendambo", "Mbunya")),
    VilleEntry("Bunia", "Ituri", listOf("Nyakasanza", "Mbunya", "Shari")),
    VilleEntry("Ariwara", "Ituri", emptyList()),
    VilleEntry("Mongwalu", "Ituri", emptyList()),
    VilleEntry("Tshikapa", "Kasaï", listOf("Dibumba I", "Dibumba II", "Kanzala", "Mabondo", "Mbumba")),
    VilleEntry("Kananga", "Kasaï-Central", listOf("Kananga", "Katoka", "Lukonga", "Ndesha", "Nganza")),
    VilleEntry("Tshimbulu", "Kasaï-Central", emptyList()),
    VilleEntry("Mbuji-Mayi", "Kasaï-Oriental", listOf("Bipemba", "Dibindi", "Diulu", "Kanshi", "Muya")),
    VilleEntry("Boma", "Kongo-Central", listOf("Kabondo", "Kalamu", "Nzadi")),
    VilleEntry("Matadi", "Kongo-Central", listOf("Matadi", "Mvuzi", "Nzanza")),
    VilleEntry("Mbanza-Ngungu", "Kongo-Central", emptyList()),
    VilleEntry("Inkisi", "Kongo-Central", emptyList()),
    VilleEntry("Kenge", "Kwango", listOf("Cinq-Mai", "Laurent-Désiré-Kabila", "Manonga", "Masikita", "Mavula")),
    VilleEntry("Bandundu", "Kwilu", listOf("Basoko", "Disasi", "Mayoyo")),
    VilleEntry("Kikwit", "Kwilu", listOf("Kazamba", "Kikwit", "Lukemi", "Lukolela", "Nzinda")),
    VilleEntry("Kabinda", "Lomami", listOf("Kabondo", "Kabuela-Buela", "Kajiba", "Mudingayi")),
    VilleEntry("Mwene-Ditu", "Lomami", listOf("Bondoyi", "Musadi", "Mwene-Ditu")),
    VilleEntry("Kolwezi", "Lualaba", listOf("Dilala", "Kolwezi", "Manika")),
    VilleEntry("Kasaji", "Lualaba", emptyList()),
    VilleEntry("Inongo", "Mai-Ndombe", listOf("Bonse", "Inongo", "Mpolo", "Mpongonzoli")),
    VilleEntry("Nioki", "Mai-Ndombe", emptyList()),
    VilleEntry("Kindu", "Maniema", listOf("Alunguli", "Kasuku", "Kindu", "Mikelenge")),
    VilleEntry("Kalima", "Maniema", emptyList()),
    VilleEntry("Lisala", "Mongala", listOf("Bolikango", "Lisala", "Mongala")),
    VilleEntry("Bumba", "Mongala", emptyList()),
    VilleEntry("Gbadolite", "Nord-Ubangi", listOf("Gbadolite", "Molegbe", "Nganza")),
    VilleEntry("Beni", "Nord-Kivu", listOf("Beu", "Bungulu", "Mulekera", "Ruwenzori")),
    VilleEntry("Butembo", "Nord-Kivu", listOf("Bulengera", "Butembo", "Kimemi", "Mususa", "Vulamba")),
    VilleEntry("Goma", "Nord-Kivu", listOf("Goma", "Karisimbi")),
    VilleEntry("Lusambo", "Sankuru", listOf("Kabondo", "Lupembe", "Lusambo", "Tusuanganyi")),
    VilleEntry("Wembo-Nyama", "Sankuru", listOf("Ewango", "Lumumbaville", "Wembo-Nyama")),
    VilleEntry("Bukavu", "Sud-Kivu", listOf("Bagira", "Ibanda", "Kadutu")),
    VilleEntry("Baraka", "Sud-Kivu", emptyList()),
    VilleEntry("Kamituga", "Sud-Kivu", emptyList()),
    VilleEntry("Uvira", "Sud-Kivu", emptyList()),
    VilleEntry("Gemena", "Sud-Ubangi", listOf("Gbazubu", "Labo", "Lac-Ntumba", "Mont-Gila")),
    VilleEntry("Zongo", "Sud-Ubangi", emptyList()),
    VilleEntry("Kalemie", "Tanganyika", listOf("Kalemie", "Lac", "Lukuga")),
    VilleEntry("Kisangani", "Tshopo", listOf("Kabondo", "Kisangani", "Lubunga", "Makiso", "Mangobo", "Tshopo")),
    VilleEntry("Yangambi", "Tshopo", emptyList()),
    VilleEntry("Boende", "Tshuapa", listOf("Boende", "Tshuapa")),
).sortedBy { it.name }

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
