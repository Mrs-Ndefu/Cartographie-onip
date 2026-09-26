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
// ou plusieurs communes de cette ville.
@Entity
@Table(name = "zones")
public class Zone {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String ville;

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

    public Zone(UUID id, String province, String ville, List<String> communes) {
        this.id = id;
        this.province = province;
        this.ville = ville;
        this.communes = new ArrayList<>(communes);
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
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

    public String getLabel() {
        return province + " / " + ville + " / " + String.join(", ", communes);
    }
}
