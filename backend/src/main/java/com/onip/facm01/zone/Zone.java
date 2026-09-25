package com.onip.facm01.zone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Zone d'affectation d'un agent, définie à l'avance par l'ADMIN : province + ville + commune,
// et optionnellement un quartier (une zone sans quartier couvre toute la commune).
@Entity
@Table(name = "zones")
public class Zone {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String ville;

    @Column(nullable = false)
    private String commune;

    private String quartier;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Zone() {
    }

    public Zone(UUID id, String province, String ville, String commune, String quartier) {
        this.id = id;
        this.province = province;
        this.ville = ville;
        this.commune = commune;
        this.quartier = quartier;
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

    public String getCommune() {
        return commune;
    }

    public String getQuartier() {
        return quartier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getLabel() {
        return Stream.of(province, ville, commune, quartier)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" / "));
    }
}
