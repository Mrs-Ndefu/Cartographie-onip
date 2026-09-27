package com.onip.facm01.household;

import java.util.Comparator;
import java.util.List;

// Villes de la RDC, leur province et leurs communes officielles — même liste que les apps de
// saisie (android-app/.../DrcLocations.kt, src/data/drcLocations.ts). Non exhaustive : les
// formulaires du tableau de bord gardent une option "Autre (saisie libre)". Sert aux listes
// déroulantes enchaînées province -> ville -> commune (cf. /dashboard/geo et app.js).
public final class DrcLocations {

    public record VilleEntry(String name, String province, List<String> communes) {
    }

    public record Geo(List<String> provinces, List<VilleEntry> villes) {
    }

    public static final List<VilleEntry> VILLES = List.of(
            new VilleEntry("Kinshasa", "KINSHASA", List.of("Bandalungwa", "Barumbu", "Bumbu", "Gombe", "Kalamu", "Kasa-Vubu", "Kimbanseke", "Kinshasa", "Kintambo", "Kisenso", "Lemba", "Limete", "Lingwala", "Makala", "Maluku", "Masina", "Matete", "Mont-Ngafula", "Ndjili", "Ngaba", "Ngaliema", "Ngiri-Ngiri", "Nsele", "Selembao")),
            new VilleEntry("Buta", "BAS-UELE", List.of("Babade", "Dobea", "Finant", "Tepatondele")),
            new VilleEntry("Aketi", "BAS-UELE", List.of()),
            new VilleEntry("Dingila", "BAS-UELE", List.of()),
            new VilleEntry("Mbandaka", "ÉQUATEUR", List.of("Mbandaka", "Wangata")),
            new VilleEntry("Likasi", "HAUT-KATANGA", List.of("Kikula", "Likasi", "Panda", "Shituru")),
            new VilleEntry("Lubumbashi", "HAUT-KATANGA", List.of("Annexe", "Kamalondo", "Kampemba", "Katuba", "Kenya", "Lubumbashi", "Ruashi")),
            new VilleEntry("Kipushi", "HAUT-KATANGA", List.of()),
            new VilleEntry("Kamina", "HAUT-LOMAMI", List.of("Dimayi", "Kamina", "Sobongo", "Kupa")),
            new VilleEntry("Isiro", "HAUT-UELE", List.of("Mambaya", "Mendambo", "Mbunya")),
            new VilleEntry("Bunia", "ITURI", List.of("Nyakasanza", "Mbunya", "Shari")),
            new VilleEntry("Ariwara", "ITURI", List.of()),
            new VilleEntry("Mongwalu", "ITURI", List.of()),
            new VilleEntry("Tshikapa", "KASAÏ", List.of("Dibumba I", "Dibumba II", "Kanzala", "Mabondo", "Mbumba")),
            new VilleEntry("Kananga", "KASAÏ-CENTRAL", List.of("Kananga", "Katoka", "Lukonga", "Ndesha", "Nganza")),
            new VilleEntry("Tshimbulu", "KASAÏ-CENTRAL", List.of()),
            new VilleEntry("Mbuji-Mayi", "KASAÏ-ORIENTAL", List.of("Bipemba", "Dibindi", "Diulu", "Kanshi", "Muya")),
            new VilleEntry("Boma", "KONGO-CENTRAL", List.of("Kabondo", "Kalamu", "Nzadi")),
            new VilleEntry("Matadi", "KONGO-CENTRAL", List.of("Matadi", "Mvuzi", "Nzanza")),
            new VilleEntry("Mbanza-Ngungu", "KONGO-CENTRAL", List.of()),
            new VilleEntry("Inkisi", "KONGO-CENTRAL", List.of()),
            new VilleEntry("Kenge", "KWANGO", List.of("Cinq-Mai", "Laurent-Désiré-Kabila", "Manonga", "Masikita", "Mavula")),
            new VilleEntry("Bandundu", "KWILU", List.of("Basoko", "Disasi", "Mayoyo")),
            new VilleEntry("Kikwit", "KWILU", List.of("Kazamba", "Kikwit", "Lukemi", "Lukolela", "Nzinda")),
            new VilleEntry("Kabinda", "LOMAMI", List.of("Kabondo", "Kabuela-Buela", "Kajiba", "Mudingayi")),
            new VilleEntry("Mwene-Ditu", "LOMAMI", List.of("Bondoyi", "Musadi", "Mwene-Ditu")),
            new VilleEntry("Kolwezi", "LUALABA", List.of("Dilala", "Kolwezi", "Manika")),
            new VilleEntry("Kasaji", "LUALABA", List.of()),
            new VilleEntry("Inongo", "MAI-NDOMBE", List.of("Bonse", "Inongo", "Mpolo", "Mpongonzoli")),
            new VilleEntry("Nioki", "MAI-NDOMBE", List.of()),
            new VilleEntry("Kindu", "MANIEMA", List.of("Alunguli", "Kasuku", "Kindu", "Mikelenge")),
            new VilleEntry("Kalima", "MANIEMA", List.of()),
            new VilleEntry("Lisala", "MONGALA", List.of("Bolikango", "Lisala", "Mongala")),
            new VilleEntry("Bumba", "MONGALA", List.of()),
            new VilleEntry("Gbadolite", "NORD-UBANGI", List.of("Gbadolite", "Molegbe", "Nganza")),
            new VilleEntry("Beni", "NORD-KIVU", List.of("Beu", "Bungulu", "Mulekera", "Ruwenzori")),
            new VilleEntry("Butembo", "NORD-KIVU", List.of("Bulengera", "Butembo", "Kimemi", "Mususa", "Vulamba")),
            new VilleEntry("Goma", "NORD-KIVU", List.of("Goma", "Karisimbi")),
            new VilleEntry("Lusambo", "SANKURU", List.of("Kabondo", "Lupembe", "Lusambo", "Tusuanganyi")),
            new VilleEntry("Wembo-Nyama", "SANKURU", List.of("Ewango", "Lumumbaville", "Wembo-Nyama")),
            new VilleEntry("Bukavu", "SUD-KIVU", List.of("Bagira", "Ibanda", "Kadutu")),
            new VilleEntry("Baraka", "SUD-KIVU", List.of()),
            new VilleEntry("Kamituga", "SUD-KIVU", List.of()),
            new VilleEntry("Uvira", "SUD-KIVU", List.of()),
            new VilleEntry("Gemena", "SUD-UBANGI", List.of("Gbazubu", "Labo", "Lac-Ntumba", "Mont-Gila")),
            new VilleEntry("Zongo", "SUD-UBANGI", List.of()),
            new VilleEntry("Kalemie", "TANGANYIKA", List.of("Kalemie", "Lac", "Lukuga")),
            new VilleEntry("Kisangani", "TSHOPO", List.of("Kabondo", "Kisangani", "Lubunga", "Makiso", "Mangobo", "Tshopo")),
            new VilleEntry("Yangambi", "TSHOPO", List.of()),
            new VilleEntry("Boende", "TSHUAPA", List.of("Boende", "Tshuapa")))
            .stream()
            .sorted(Comparator.comparing(VilleEntry::name))
            .toList();

    public static final Geo GEO = new Geo(DrcProvinces.PROVINCES, VILLES);

    private DrcLocations() {
    }
}
