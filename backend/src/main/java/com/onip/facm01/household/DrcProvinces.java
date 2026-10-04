package com.onip.facm01.household;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Les 26 provinces de la RDC. Sert à proposer la liste des provinces dans le tableau de bord, et à
// déduire la province d'un ménage envoyé sans province (anciennes versions des apps, ménages déjà
// enregistrés) à partir de sa ville (cf. DrcLocations).
public final class DrcProvinces {

    public static final List<String> PROVINCES = List.of(
            "BAS-UELE", "ÉQUATEUR", "HAUT-KATANGA", "HAUT-LOMAMI", "HAUT-UELE", "ITURI", "KASAÏ",
            "KASAÏ-CENTRAL", "KASAÏ-ORIENTAL", "KINSHASA", "KONGO-CENTRAL", "KWANGO", "KWILU", "LOMAMI",
            "LUALABA", "MAI-NDOMBE", "MANIEMA", "MONGALA", "NORD-KIVU", "NORD-UBANGI", "SANKURU",
            "SUD-KIVU", "SUD-UBANGI", "TANGANYIKA", "TSHOPO", "TSHUAPA");

    // Lettre fixe de chaque province, pour les codes de zone (A1, A2, B1...) : A = Kinshasa, puis
    // les 25 autres provinces dans l'ordre alphabétique (B = Bas-Uele ... Z = Tshuapa).
    public static final List<String> PROVINCES_BY_LETTER = Stream.concat(
                    Stream.of("KINSHASA"),
                    PROVINCES.stream().filter(p -> !p.equals("KINSHASA")))
            .toList();

    // Clé sans accents : "Kasaï" saisi "KASAI" doit retrouver la même province.
    private static final Map<String, String> PROVINCE_BY_KEY = PROVINCES.stream()
            .collect(Collectors.toMap(DrcProvinces::key, p -> p));

    private DrcProvinces() {
    }

    /** Lettre de la province (A = Kinshasa...), ou vide pour une province hors de la liste. */
    public static Optional<String> letterOf(String province) {
        if (province == null || province.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(province);
        int index = PROVINCES_BY_LETTER.indexOf(normalized);
        return index < 0 ? Optional.empty() : Optional.of(String.valueOf((char) ('A' + index)));
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
