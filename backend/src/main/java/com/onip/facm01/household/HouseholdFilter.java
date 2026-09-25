package com.onip.facm01.household;

import java.time.Instant;

// Filtres du tableau de bord des ménages. Tous optionnels : null ou vide = pas de filtre.
// statut vaut "complet", "incomplet" (tout sauf complet) ou vide.
public record HouseholdFilter(
        String search,
        String province,
        String ville,
        String commune,
        String quartier,
        String statut,
        Instant dateFrom,
        Instant dateTo) {

    // Un filtre géographique est actif : la carte doit alors zoomer sur les ménages trouvés.
    public boolean hasPlaceFilter() {
        return notBlank(province) || notBlank(ville) || notBlank(commune) || notBlank(quartier);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
