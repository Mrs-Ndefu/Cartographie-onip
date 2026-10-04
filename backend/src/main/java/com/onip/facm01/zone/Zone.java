package com.onip.facm01.zone;

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
import java.util.List;
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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Zone() {
    }

    public Zone(UUID id, String code, String province, String ville, List<String> communes) {
        this.id = id;
        this.code = code;
        this.province = province;
        this.ville = ville;
        this.communes = new ArrayList<>(communes);
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

    public String getVille() {
        return ville;
    }

    public List<String> getCommunes() {
        return communes;
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
