package com.onip.facm01.household;

import java.time.Instant;
import java.util.List;

// Filtres du tableau de bord des ménages. Tous optionnels : null ou vide = pas de filtre.
// communes : les communes d'une zone choisie (le ménage doit être dans l'une d'elles).
// statut vaut "complet", "incomplet" (tout sauf complet) ou vide.
public record HouseholdFilter(
        String search,
        String province,
        String ville,
        String commune,
        List<String> communes,
        String statut,
        Instant dateFrom,
        Instant dateTo) {

    // Un filtre géographique est actif : la carte doit alors zoomer sur les ménages trouvés.
    public boolean hasPlaceFilter() {
        return notBlank(province) || notBlank(ville) || notBlank(commune) || (communes != null && !communes.isEmpty());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
