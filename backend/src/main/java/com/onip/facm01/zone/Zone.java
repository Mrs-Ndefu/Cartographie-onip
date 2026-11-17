package com.onip.facm01.zone;

import com.onip.facm01.household.DrcProvinces;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Zone d'affectation d'un agent, définie à l'avance par l'ADMIN : une province, une ville, et une
// ou plusieurs communes de cette ville. Chaque zone porte un code attribué à la création : la
// lettre de sa province (A = Kinshasa...) et un numéro dans la province (A1, A2, B1...) — c'est
// ainsi que l'agent la désigne sur le terrain.
@Entity
@Table(name = "zones")
public class Zone {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String ville;

    // Code de la zone (A1, A2, B1...), cf. ZoneService.nextCode. Null pour une province hors de
    // la liste de référence.
    @Column(length = 4)
    private String code;

    // Chargées d'office : une zone est toujours affichée avec ses communes, et la liste est courte.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "zone_communes", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "commune", nullable = false)
    @OrderBy
    private List<String> communes = new ArrayList<>();

    // Quartiers de la zone (au moins un pour une zone créée ou complétée depuis l'ajout des
    // quartiers ; vide = zone ancienne « à compléter »). Un Set et non une List : Hibernate refuse
    // deux listes chargées d'office sur la même entité (MultipleBagFetchException).
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "zone_quartiers", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "quartier", nullable = false)
    private Set<String> quartiers = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Zone() {
    }

    public Zone(UUID id, String code, String province, String ville, List<String> communes) {
        this(id, code, province, ville, communes, List.of());
    }

    public Zone(UUID id, String code, String province, String ville, List<String> communes, List<String> quartiers) {
        this.id = id;
        this.code = code;
        this.province = province;
        this.ville = ville;
        this.communes = new ArrayList<>(communes);
        this.quartiers = new LinkedHashSet<>(quartiers);
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    // "Zone A1" ; repli sur le libellé géographique si la zone n'a pas de code.
    public String getName() {
        return code == null ? getPlace() : "Zone " + code;
    }

    public String getProvince() {
        return province;
    }

    // Numéro de la province sur 2 chiffres (01 = Kinshasa, puis les 25 autres par ordre
    // alphabétique — cf. DrcProvinces.numberOf), pour l'affichage dans la page Zones à côté du
    // nom de la province. '' si la province n'est pas reconnue (ne devrait pas arriver, la
    // création d'une zone impose une province de la liste).
    public String getProvinceNumber() {
        return DrcProvinces.numberOf(province).orElse("");
    }

    public String getVille() {
        return ville;
    }

    public List<String> getCommunes() {
        return communes;
    }

    // Triés pour l'affichage.
    public List<String> getQuartiers() {
        return quartiers.stream().sorted().toList();
    }

    public void setQuartiers(List<String> quartiers) {
        this.quartiers.clear();
        this.quartiers.addAll(quartiers);
    }

    // Zone créée avant l'ajout des quartiers : à compléter par l'ADMIN.
    public boolean isQuartiersManquants() {
        return quartiers.isEmpty();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Province / ville / communes.
    public String getPlace() {
        return province + " / " + ville + " / " + String.join(", ", communes);
    }

    // "Zone A1 — KINSHASA / KINSHASA / GOMBE, LINGWALA" (listes, messages, historique).
    public String getLabel() {
        return code == null ? getPlace() : "Zone " + code + " — " + getPlace();
    }
}
