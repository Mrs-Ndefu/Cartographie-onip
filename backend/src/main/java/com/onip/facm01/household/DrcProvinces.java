package com.onip.facm01.household;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Les 26 provinces de la RDC. Sert à proposer la liste des provinces dans le tableau de bord, et à
// déduire la province d'un ménage envoyé sans province (anciennes versions des apps, ménages déjà
// enregistrés) à partir de sa ville (cf. DrcLocations).
public final class DrcProvinces {

    public static final List<String> PROVINCES = List.of(
            "BAS-UELE", "ÉQUATEUR", "HAUT-KATANGA", "HAUT-LOMAMI", "HAUT-UELE", "ITURI", "KASAÏ",
            "KASAÏ-CENTRAL", "KASAÏ-ORIENTAL", "KINSHASA", "KONGO-CENTRAL", "KWANGO", "KWILU", "LOMAMI",
            "LUALABA", "MAI-NDOMBE", "MANIEMA", "MONGALA", "NORD-KIVU", "NORD-UBANGI", "SANKURU",
            "SUD-KIVU", "SUD-UBANGI", "TANGANYIKA", "TSHOPO", "TSHUAPA");

    // Clé sans accents : "Kasaï" saisi "KASAI" doit retrouver la même province.
    private static final Map<String, String> PROVINCE_BY_KEY = PROVINCES.stream()
            .collect(Collectors.toMap(DrcProvinces::key, p -> p));

    private DrcProvinces() {
    }

    public static Optional<String> fromVille(String ville) {
        if (ville == null || ville.isBlank()) {
            return Optional.empty();
        }
        return DrcLocations.VILLES.stream()
                .filter(v -> key(v.name()).equals(key(ville)))
                .map(DrcLocations.VilleEntry::province)
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
