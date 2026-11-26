package com.onip.facm01.site;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Questionnaire terrain de la fiche de repérage (« Questionnaire Terrain Bureaux État Civil ONIP »),
 * décrit dans /questionnaire/questionnaire-site.json — le même fichier que l'app mobile
 * (assets/questionnaire-site.json) : sections, questions, choix et sauts (« Si Q1 = ... → Qx »).
 *
 * Chaque réponse a une clé : l'id de la question (ex. "q8"), "id_ligne" pour un tableau (ex.
 * "q21_portes"), et la clé de précision d'un choix « Autre (à préciser) ». Les questions marquées
 * "bound" correspondent à une colonne existante de {@link Site} (ex. typeSite pour Q1) ; toutes
 * les autres sont rangées ensemble dans la colonne JSON {@code Site.questionnaire}.
 * Les choix multiples sont stockés "A|B", les oui/non "OUI"/"NON".
 */
@Component
public class SiteQuestionnaire {

    public record Option(String value, String label) {
    }

    public record Item(String id, String num, String label, String type, List<Option> options,
                       List<Option> legacyOptions, List<String> precise, String precisionId, boolean bound,
                       Map<String, List<String>> showIf, Map<String, List<String>> hideIf,
                       List<Option> columns, List<Option> rows) {

        public String displayLabel() {
            return num == null || num.isBlank() ? label : num + ". " + label;
        }

        public boolean isNote() {
            return "note".equals(type);
        }

        public boolean hasPrecision() {
            return !precise.isEmpty();
        }

        public String precisionKey() {
            return precisionId != null ? precisionId : id + "_precision";
        }

        public String rowKey(String row) {
            return id + "_" + row;
        }

        public String optionLabel(String value) {
            for (Option o : options) {
                if (o.value().equals(value)) return o.label();
            }
            for (Option o : legacyOptions) {
                if (o.value().equals(value)) return o.label();
            }
            for (Option o : columns) {
                if (o.value().equals(value)) return o.label();
            }
            if ("yesno".equals(type)) {
                return "OUI".equals(value) ? "Oui" : "NON".equals(value) ? "Non" : value;
            }
            return value;
        }

        /** Clés de réponse de cette question (vide pour une simple note). */
        public List<String> keys() {
            if (isNote()) return List.of();
            if ("grid".equals(type)) return rows.stream().map(r -> rowKey(r.value())).toList();
            return hasPrecision() ? List.of(id, precisionKey()) : List.of(id);
        }
    }

    public record Section(String id, String title, Map<String, List<String>> hideIf, List<Item> items) {
    }

    /** Ligne de la fiche détail : question, réponse lisible ("—" si vide), ou intertitre (note). */
    public record FieldView(String label, String value, boolean note) {
    }

    public record SectionView(String title, List<FieldView> fields) {
    }

    // Colonnes de Site reliées à une question (cf. get/set) — vérifié au démarrage contre le JSON.
    private static final Set<String> TYPED_KEYS = Set.of(
            "typeSite", "typeSiteAutre", "optionOccupation", "disponibiliteImmediate", "delaiEstime",
            "proprietaireNomContact", "proprieteStatut", "dateEvaluation", "nomSignatureEnqueteur",
            "etatBatiment", "superficieTerrainM2", "superficieBatimentM2", "regideso", "sanitaires",
            "nombrePieces", "connectiviteInternet", "proximitePolice", "observationsParticulieres",
            "typeInfrastructure", "typeInfrastructureAutre", "etatBatimentObservations", "accesRoutier",
            "raccordeSnel", "stabiliteSnel", "groupeElectrogeneSolaire", "nonElectrifie", "reseauxDisponibles",
            "clotureSecurise", "distancePoliceMetres", "avisGlobal", "travauxAmenagement", "motifDefavorable");

    private static final int MAX_VALUE_LENGTH = 4000;

    private final ObjectMapper objectMapper;
    private final List<Section> sections;
    private final Set<String> boundKeys = new LinkedHashSet<>();
    private final Set<String> jsonKeys = new LinkedHashSet<>();

    public SiteQuestionnaire(ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        try (InputStream in = getClass().getResourceAsStream("/questionnaire/questionnaire-site.json")) {
            if (in == null) throw new IllegalStateException("questionnaire-site.json introuvable");
            this.sections = parse(objectMapper.readTree(in));
        }
        for (Section section : sections) {
            for (Item item : section.items()) {
                if (item.bound()) {
                    boundKeys.addAll(item.keys());
                } else {
                    jsonKeys.addAll(item.keys());
                }
            }
        }
        for (String key : boundKeys) {
            if (!TYPED_KEYS.contains(key)) {
                throw new IllegalStateException("Question reliée à une colonne inconnue : " + key);
            }
        }
    }

    public List<Section> sections() {
        return sections;
    }

    // ------------------------------------------------------------------ lecture

    /** Toutes les réponses d'une fiche : colonnes dédiées + JSON. */
    public Map<String, String> answers(Site site) {
        Map<String, String> answers = new LinkedHashMap<>(readJson(site.getQuestionnaire()));
        for (String key : boundKeys) {
            String value = get(site, key);
            if (value != null && !value.isBlank()) answers.put(key, value);
        }
        return answers;
    }

    /** Fiche détail du tableau de bord : questions visibles (sauts appliqués), réponses lisibles. */
    public List<SectionView> view(Site site) {
        Map<String, String> answers = answers(site);
        List<SectionView> views = new ArrayList<>();
        for (Section section : sections) {
            if (hiddenBy(section.hideIf(), answers)) continue;
            List<FieldView> fields = new ArrayList<>();
            for (Item item : section.items()) {
                if (!visible(item, answers)) continue;
                if (item.isNote()) {
                    fields.add(new FieldView(item.displayLabel(), null, true));
                } else if ("grid".equals(item.type())) {
                    fields.add(new FieldView(item.displayLabel(), null, true));
                    for (Option row : item.rows()) {
                        fields.add(new FieldView(row.label(), readable(item, answers.get(item.rowKey(row.value()))), false));
                    }
                } else {
                    String value = readable(item, answers.get(item.id()));
                    if (item.hasPrecision()) {
                        String precision = answers.get(item.precisionKey());
                        if (precision != null && !precision.isBlank()) value = value + " — " + precision;
                    }
                    fields.add(new FieldView(item.displayLabel(), value, false));
                }
            }
            if (!fields.isEmpty()) views.add(new SectionView(section.title(), fields));
        }
        return views;
    }

    private static String readable(Item item, String raw) {
        if (raw == null || raw.isBlank()) return "—";
        if ("choice".equals(item.type()) || "yesno".equals(item.type()) || "grid".equals(item.type())) {
            return item.optionLabel(raw);
        }
        if ("multi".equals(item.type())) {
            return String.join(", ", Arrays.stream(raw.split("\\|")).filter(v -> !v.isBlank()).map(item::optionLabel).toList());
        }
        return raw;
    }

    // Sauts du questionnaire : showIf = toutes les conditions remplies, hideIf = l'une d'elles.
    // Une question dont la réponse de référence est vide reste affichée (rien à sauter encore).
    public static boolean visible(Item item, Map<String, String> answers) {
        if (hiddenBy(item.hideIf(), answers)) return false;
        for (Map.Entry<String, List<String>> cond : item.showIf().entrySet()) {
            if (!matches(answers.get(cond.getKey()), cond.getValue())) return false;
        }
        return true;
    }

    public static boolean hiddenBy(Map<String, List<String>> hideIf, Map<String, String> answers) {
        for (Map.Entry<String, List<String>> cond : hideIf.entrySet()) {
            if (matches(answers.get(cond.getKey()), cond.getValue())) return true;
        }
        return false;
    }

    private static boolean matches(String answer, List<String> values) {
        if (answer == null || answer.isBlank()) return false;
        for (String part : answer.split("\\|")) {
            if (values.contains(part)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ écriture

    /** Formulaire complet du tableau de bord : chaque clé absente ou vide efface la réponse. */
    public void apply(Site site, Map<String, String> answers) {
        for (String key : boundKeys) {
            set(site, key, clean(answers.get(key)));
        }
        Map<String, String> json = new TreeMap<>();
        for (String key : jsonKeys) {
            String value = clean(answers.get(key));
            if (value != null) json.put(key, value);
        }
        site.setQuestionnaire(writeJson(json));
    }

    /**
     * Synchronisation mobile : les colonnes dédiées arrivent par leurs propres champs, on ne garde
     * ici que les réponses connues sans colonne. null = l'app n'envoie pas de questionnaire
     * (ancienne version) : on ne touche pas aux réponses déjà en base.
     */
    public void applySync(Site site, Map<String, String> questionnaire) {
        if (questionnaire == null) return;
        Map<String, String> json = new TreeMap<>();
        for (String key : jsonKeys) {
            String value = clean(questionnaire.get(key));
            if (value != null) json.put(key, value);
        }
        site.setQuestionnaire(writeJson(json));
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() > MAX_VALUE_LENGTH ? trimmed.substring(0, MAX_VALUE_LENGTH) : trimmed;
    }

    private Map<String, String> readJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() { });
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    private String writeJson(Map<String, String> json) {
        if (json.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(json);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ colonnes dédiées

    private static String get(Site s, String key) {
        return switch (key) {
            case "typeSite" -> name(s.getTypeSite());
            case "typeSiteAutre" -> s.getTypeSiteAutre();
            case "optionOccupation" -> name(s.getOptionOccupation());
            case "disponibiliteImmediate" -> yesNo(s.getDisponibiliteImmediate());
            case "delaiEstime" -> s.getDelaiEstime();
            case "proprietaireNomContact" -> s.getProprietaireNomContact();
            case "proprieteStatut" -> name(s.getProprieteStatut());
            case "dateEvaluation" -> s.getDateEvaluation() == null ? null : s.getDateEvaluation().toString();
            case "nomSignatureEnqueteur" -> s.getNomSignatureEnqueteur();
            case "etatBatiment" -> name(s.getEtatBatiment());
            case "superficieTerrainM2" -> number(s.getSuperficieTerrainM2());
            case "superficieBatimentM2" -> number(s.getSuperficieBatimentM2());
            case "regideso" -> yesNo(s.getRegideso());
            case "sanitaires" -> yesNo(s.getSanitaires());
            case "nombrePieces" -> s.getNombrePieces() == null ? null : s.getNombrePieces().toString();
            case "connectiviteInternet" -> name(s.getConnectiviteInternet());
            case "proximitePolice" -> yesNo(s.getProximitePolice());
            case "observationsParticulieres" -> s.getObservationsParticulieres();
            case "typeInfrastructure" -> name(s.getTypeInfrastructure());
            case "typeInfrastructureAutre" -> s.getTypeInfrastructureAutre();
            case "etatBatimentObservations" -> s.getEtatBatimentObservations();
            case "accesRoutier" -> name(s.getAccesRoutier());
            case "raccordeSnel" -> yesNo(s.getRaccordeSnel());
            case "stabiliteSnel" -> name(s.getStabiliteSnel());
            case "groupeElectrogeneSolaire" -> yesNo(s.getGroupeElectrogeneSolaire());
            case "nonElectrifie" -> yesNo(s.getNonElectrifie());
            case "reseauxDisponibles" -> s.getReseauxDisponibles();
            case "clotureSecurise" -> yesNo(s.getClotureSecurise());
            case "distancePoliceMetres" -> s.getDistancePoliceMetres() == null ? null : s.getDistancePoliceMetres().toString();
            case "avisGlobal" -> name(s.getAvisGlobal());
            case "travauxAmenagement" -> s.getTravauxAmenagement();
            case "motifDefavorable" -> s.getMotifDefavorable();
            default -> throw new IllegalStateException("Colonne inconnue : " + key);
        };
    }

    private static void set(Site s, String key, String v) {
        switch (key) {
            case "typeSite" -> s.setTypeSite(enumOf(TypeSite.class, v));
            case "typeSiteAutre" -> s.setTypeSiteAutre(v);
            case "optionOccupation" -> s.setOptionOccupation(enumOf(OptionOccupation.class, v));
            case "disponibiliteImmediate" -> s.setDisponibiliteImmediate(bool(v));
            case "delaiEstime" -> s.setDelaiEstime(v);
            case "proprietaireNomContact" -> s.setProprietaireNomContact(v);
            case "proprieteStatut" -> s.setProprieteStatut(enumOf(ProprieteStatut.class, v));
            case "dateEvaluation" -> s.setDateEvaluation(date(v));
            case "nomSignatureEnqueteur" -> s.setNomSignatureEnqueteur(v);
            case "etatBatiment" -> s.setEtatBatiment(enumOf(EtatBatiment.class, v));
            case "superficieTerrainM2" -> s.setSuperficieTerrainM2(decimal(v));
            case "superficieBatimentM2" -> s.setSuperficieBatimentM2(decimal(v));
            case "regideso" -> s.setRegideso(bool(v));
            case "sanitaires" -> s.setSanitaires(bool(v));
            case "nombrePieces" -> s.setNombrePieces(integer(v));
            case "connectiviteInternet" -> s.setConnectiviteInternet(enumOf(ConnectiviteInternet.class, v));
            case "proximitePolice" -> s.setProximitePolice(bool(v));
            case "observationsParticulieres" -> s.setObservationsParticulieres(v);
            case "typeInfrastructure" -> s.setTypeInfrastructure(enumOf(TypeInfrastructure.class, v));
            case "typeInfrastructureAutre" -> s.setTypeInfrastructureAutre(v);
            case "etatBatimentObservations" -> s.setEtatBatimentObservations(v);
            case "accesRoutier" -> s.setAccesRoutier(enumOf(AccesRoutier.class, v));
            case "raccordeSnel" -> s.setRaccordeSnel(bool(v));
            case "stabiliteSnel" -> s.setStabiliteSnel(enumOf(StabiliteSnel.class, v));
            case "groupeElectrogeneSolaire" -> s.setGroupeElectrogeneSolaire(bool(v));
            case "nonElectrifie" -> s.setNonElectrifie(bool(v));
            case "reseauxDisponibles" -> s.setReseauxDisponibles(v);
            case "clotureSecurise" -> s.setClotureSecurise(bool(v));
            case "distancePoliceMetres" -> s.setDistancePoliceMetres(integer(v));
            case "avisGlobal" -> s.setAvisGlobal(enumOf(AvisGlobal.class, v));
            case "travauxAmenagement" -> s.setTravauxAmenagement(v);
            case "motifDefavorable" -> s.setMotifDefavorable(v);
            default -> throw new IllegalStateException("Colonne inconnue : " + key);
        }
    }

    private static String name(Enum<?> e) {
        return e == null ? null : e.name();
    }

    private static String yesNo(Boolean b) {
        return b == null ? null : (b ? "OUI" : "NON");
    }

    private static Boolean bool(String v) {
        return v == null ? null : "OUI".equals(v) ? Boolean.TRUE : "NON".equals(v) ? Boolean.FALSE : null;
    }

    private static String number(Double d) {
        if (d == null) return null;
        return d == Math.rint(d) ? String.valueOf(d.longValue()) : d.toString();
    }

    private static Double decimal(String v) {
        if (v == null) return null;
        try {
            return Double.valueOf(v.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer integer(String v) {
        if (v == null) return null;
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate date(String v) {
        if (v == null) return null;
        try {
            return LocalDate.parse(v);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String v) {
        if (v == null) return null;
        try {
            return Enum.valueOf(type, v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ lecture du JSON

    private static List<Section> parse(JsonNode root) {
        List<Section> result = new ArrayList<>();
        for (JsonNode s : root.path("sections")) {
            List<Item> items = new ArrayList<>();
            for (JsonNode i : s.path("items")) {
                items.add(new Item(
                        i.path("id").asText(""),
                        i.hasNonNull("num") ? i.get("num").asText() : null,
                        i.path("label").asText(""),
                        i.path("type").asText("text"),
                        options(i.path("options")),
                        options(i.path("legacyOptions")),
                        strings(i.path("precise")),
                        i.hasNonNull("precisionId") ? i.get("precisionId").asText() : null,
                        i.path("bound").asBoolean(false),
                        conditions(i.path("showIf")),
                        conditions(i.path("hideIf")),
                        options(i.path("columns")),
                        options(i.path("rows"))));
            }
            result.add(new Section(s.path("id").asText(), s.path("title").asText(), conditions(s.path("hideIf")), List.copyOf(items)));
        }
        return List.copyOf(result);
    }

    private static List<Option> options(JsonNode node) {
        List<Option> list = new ArrayList<>();
        for (JsonNode pair : node) {
            list.add(new Option(pair.get(0).asText(), pair.get(1).asText()));
        }
        return List.copyOf(list);
    }

    private static List<String> strings(JsonNode node) {
        List<String> list = new ArrayList<>();
        node.forEach(n -> list.add(n.asText()));
        return List.copyOf(list);
    }

    private static Map<String, List<String>> conditions(JsonNode node) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        node.fields().forEachRemaining(e -> map.put(e.getKey(), strings(e.getValue())));
        return Collections.unmodifiableMap(map);
    }
}
