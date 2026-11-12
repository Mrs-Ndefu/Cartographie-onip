package com.onip.facm01.zone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

// Site importé depuis un fichier Excel par l'ADMIN (page Zones > « Ajout site ») : nom, adresse,
// province, ville et territoire. Distinct des fiches de repérage (com.onip.facm01.site.Site), qui
// sont remplies sur le terrain. Chaque site est rattaché à une zone d'affectation — automatiquement
// à l'import (province + territoire = province + ville/territoire de la zone, cf.
// ZoneSiteService.matchZone), ou à la main par l'ADMIN.
@Entity
@Table(name = "zone_sites")
public class ZoneSite {

    @Id
    private UUID id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "province")
    private String province;

    @Column(name = "ville")
    private String ville;

    @Column(name = "territoire")
    private String territoire;

    // Commune/secteur/chefferie : colonne optionnelle de l'Excel, ou choisie dans « Modifier le
    // site » (listes Province -> Ville/Territoire -> Commune).
    @Column(name = "commune")
    private String commune;

    // Null = site non affecté (aucune zone ne correspond, ou zone supprimée depuis).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    // true quand la zone a été choisie à la main : la réaffectation automatique n'y touche plus.
    @Column(name = "zone_manuelle", nullable = false)
    private boolean zoneManuelle;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ZoneSite() {
    }

    public ZoneSite(UUID id, String nom, String adresse, String province, String ville, String territoire, String commune) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.province = province;
        this.ville = ville;
        this.territoire = territoire;
        this.commune = commune;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getAdresse() {
        return adresse;
    }

    public String getProvince() {
        return province;
    }

    public String getVille() {
        return ville;
    }

    public String getTerritoire() {
        return territoire;
    }

    // Modification depuis la page Sites (ADMIN).
    public void update(String nom, String adresse, String province, String ville, String territoire, String commune) {
        this.nom = nom;
        this.adresse = adresse;
        this.province = province;
        this.ville = ville;
        this.territoire = territoire;
        this.commune = commune;
    }

    public String getCommune() {
        return commune;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public boolean isZoneManuelle() {
        return zoneManuelle;
    }

    public void setZoneManuelle(boolean zoneManuelle) {
        this.zoneManuelle = zoneManuelle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
