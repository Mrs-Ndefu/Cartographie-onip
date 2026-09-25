package com.onip.facm01.household;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Les 26 provinces de la RDC, et la province de chaque ville proposée par les apps terrain (même
// source que android-app/.../DrcLocations.kt et src/data/drcLocations.ts). Sert à proposer la
// liste des provinces dans le tableau de bord, et à déduire la province d'un ménage envoyé sans
// province (anciennes versions des apps, ménages déjà enregistrés).
public final class DrcProvinces {

    public static final List<String> PROVINCES = List.of(
            "BAS-UELE", "ÉQUATEUR", "HAUT-KATANGA", "HAUT-LOMAMI", "HAUT-UELE", "ITURI", "KASAÏ",
            "KASAÏ-CENTRAL", "KASAÏ-ORIENTAL", "KINSHASA", "KONGO-CENTRAL", "KWANGO", "KWILU", "LOMAMI",
            "LUALABA", "MAI-NDOMBE", "MANIEMA", "MONGALA", "NORD-KIVU", "NORD-UBANGI", "SANKURU",
            "SUD-KIVU", "SUD-UBANGI", "TANGANYIKA", "TSHOPO", "TSHUAPA");

    private static final Map<String, String> PROVINCE_BY_VILLE = Map.ofEntries(
            Map.entry("KINSHASA", "KINSHASA"),
            Map.entry("BUTA", "BAS-UELE"), Map.entry("AKETI", "BAS-UELE"), Map.entry("DINGILA", "BAS-UELE"),
            Map.entry("MBANDAKA", "ÉQUATEUR"),
            Map.entry("LIKASI", "HAUT-KATANGA"), Map.entry("LUBUMBASHI", "HAUT-KATANGA"), Map.entry("KIPUSHI", "HAUT-KATANGA"),
            Map.entry("KAMINA", "HAUT-LOMAMI"),
            Map.entry("ISIRO", "HAUT-UELE"),
            Map.entry("BUNIA", "ITURI"), Map.entry("ARIWARA", "ITURI"), Map.entry("MONGWALU", "ITURI"),
            Map.entry("TSHIKAPA", "KASAÏ"),
            Map.entry("KANANGA", "KASAÏ-CENTRAL"), Map.entry("TSHIMBULU", "KASAÏ-CENTRAL"),
            Map.entry("MBUJI-MAYI", "KASAÏ-ORIENTAL"),
            Map.entry("BOMA", "KONGO-CENTRAL"), Map.entry("MATADI", "KONGO-CENTRAL"),
            Map.entry("MBANZA-NGUNGU", "KONGO-CENTRAL"), Map.entry("INKISI", "KONGO-CENTRAL"),
            Map.entry("KENGE", "KWANGO"),
            Map.entry("BANDUNDU", "KWILU"), Map.entry("KIKWIT", "KWILU"),
            Map.entry("KABINDA", "LOMAMI"), Map.entry("MWENE-DITU", "LOMAMI"),
            Map.entry("KOLWEZI", "LUALABA"), Map.entry("KASAJI", "LUALABA"),
            Map.entry("INONGO", "MAI-NDOMBE"), Map.entry("NIOKI", "MAI-NDOMBE"),
            Map.entry("KINDU", "MANIEMA"), Map.entry("KALIMA", "MANIEMA"),
            Map.entry("LISALA", "MONGALA"), Map.entry("BUMBA", "MONGALA"),
            Map.entry("GBADOLITE", "NORD-UBANGI"),
            Map.entry("BENI", "NORD-KIVU"), Map.entry("BUTEMBO", "NORD-KIVU"), Map.entry("GOMA", "NORD-KIVU"),
            Map.entry("LUSAMBO", "SANKURU"), Map.entry("WEMBO-NYAMA", "SANKURU"),
            Map.entry("BUKAVU", "SUD-KIVU"), Map.entry("BARAKA", "SUD-KIVU"),
            Map.entry("KAMITUGA", "SUD-KIVU"), Map.entry("UVIRA", "SUD-KIVU"),
            Map.entry("GEMENA", "SUD-UBANGI"), Map.entry("ZONGO", "SUD-UBANGI"),
            Map.entry("KALEMIE", "TANGANYIKA"),
            Map.entry("KISANGANI", "TSHOPO"), Map.entry("YANGAMBI", "TSHOPO"),
            Map.entry("BOENDE", "TSHUAPA"));

    // Clé sans accents : "Kasaï" saisi "KASAI" doit retrouver la même province.
    private static final Map<String, String> PROVINCE_BY_KEY = PROVINCES.stream()
            .collect(Collectors.toMap(DrcProvinces::key, p -> p));

    private DrcProvinces() {
    }

    public static Optional<String> fromVille(String ville) {
        if (ville == null || ville.isBlank()) {
            return Optional.empty();
        }
        return PROVINCE_BY_VILLE.entrySet().stream()
                .filter(e -> key(e.getKey()).equals(key(ville)))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    // Ramène une province saisie à son orthographe de référence (majuscules, accents) ; une
    // valeur inconnue est gardée telle quelle, en majuscules.
    public static String normalize(String province) {
        if (province == null || province.isBlank()) {
            return null;
        }
        return PROVINCE_BY_KEY.getOrDefault(key(province), province.trim().toUpperCase());
    }

    // Province à enregistrer pour une adresse : celle envoyée si elle existe, sinon déduite de la
    // ville.
    public static String resolve(String province, String ville) {
        String normalized = normalize(province);
        return normalized != null ? normalized : fromVille(ville).orElse(null);
    }

    private static String key(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();
    }
}
