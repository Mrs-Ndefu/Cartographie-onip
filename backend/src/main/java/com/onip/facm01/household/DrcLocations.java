package com.onip.facm01.household;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Référentiel géographique de la RDC : provinces -> villes/territoires -> communes/cités/secteurs
// -> quartiers. Chargé depuis resources/geo/drc-geo.json, généré à partir du fichier
// « Subdivision ADM-RDC » (Carto-Begins) fusionné avec les listes de villes/communes des apps ;
// la même copie est embarquée dans l'app Android (assets) et l'app web (src/data). Non
// exhaustif : les formulaires gardent une option « Autre (saisie libre) ». Sert aux listes
// déroulantes enchaînées province -> ville -> commune -> quartier (cf. /dashboard/geo et app.js).
public final class DrcLocations {

    // quartiers : commune -> quartiers de cette commune (vide si non renseignés).
    public record VilleEntry(String name, String province, List<String> communes, Map<String, List<String>> quartiers) {
    }

    public record Geo(List<String> provinces, List<VilleEntry> villes) {
    }

    public static final List<VilleEntry> VILLES = load();

    public static final Geo GEO = new Geo(DrcProvinces.PROVINCES, VILLES);

    private DrcLocations() {
    }

    private static List<VilleEntry> load() {
        try (InputStream in = DrcLocations.class.getResourceAsStream("/geo/drc-geo.json")) {
            if (in == null) {
                throw new IllegalStateException("Référentiel géographique introuvable : /geo/drc-geo.json");
            }
            JsonNode root = new ObjectMapper().readTree(in);
            List<VilleEntry> villes = new ArrayList<>();
            for (JsonNode v : root.path("villes")) {
                List<String> communes = new ArrayList<>();
                Map<String, List<String>> quartiers = new LinkedHashMap<>();
                for (JsonNode c : v.path("communes")) {
                    String commune = c.path("name").asText();
                    communes.add(commune);
                    List<String> qs = new ArrayList<>();
                    c.path("quartiers").forEach(q -> qs.add(q.asText()));
                    if (!qs.isEmpty()) {
                        quartiers.put(commune, List.copyOf(qs));
                    }
                }
                villes.add(new VilleEntry(
                        v.path("name").asText(),
                        // Provinces en majuscules, orthographe de référence (cf. DrcProvinces).
                        DrcProvinces.normalize(v.path("province").asText().toUpperCase(Locale.FRENCH)),
                        List.copyOf(communes),
                        Map.copyOf(quartiers)));
            }
            villes.sort(Comparator.comparing(VilleEntry::name, String.CASE_INSENSITIVE_ORDER));
            return List.copyOf(villes);
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du référentiel géographique impossible", e);
        }
    }
}
