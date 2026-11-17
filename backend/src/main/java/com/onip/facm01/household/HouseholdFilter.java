package com.onip.facm01.household;

import java.time.Instant;
import java.util.List;

// Filtres du tableau de bord des ménages. Tous optionnels : null ou vide = pas de filtre.
// quartier : quartier/groupement de l'adresse (référentiel « Subdivision ADM-RDC »).
// communes : les communes d'une zone choisie (le ménage doit être dans l'une d'elles).
// statut vaut "complet", "incomplet" (tout sauf complet) ou vide.
public record HouseholdFilter(
        String search,
        String province,
        String ville,
        String commune,
        String quartier,
        List<String> communes,
        // quartiers de la zone choisie (vide/null = toute la commune)
        List<String> quartiers,
        String statut,
        Instant dateFrom,
        Instant dateTo) {

    // Un filtre géographique est actif : la carte doit alors zoomer sur les ménages trouvés.
    public boolean hasPlaceFilter() {
        return notBlank(province) || notBlank(ville) || notBlank(commune) || notBlank(quartier)
                || (communes != null && !communes.isEmpty());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
