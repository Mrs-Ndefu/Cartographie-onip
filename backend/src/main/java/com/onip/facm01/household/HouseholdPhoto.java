package com.onip.facm01.household;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

// Une ligne par photo (jusqu'à 4 par ménage, cf. HouseholdService.MAX_PHOTOS) — ses octets ne
// doivent être chargés que sur demande explicite (une photo à la fois), jamais entraînés par une
// requête de liste/recherche portant sur Household. Voir le commentaire sur Household.photoCount.
@Entity
@Table(name = "household_photos")
public class HouseholdPhoto {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    // Ordre d'affichage dans la galerie (0 à 3) — pas de sens métier au-delà de ça.
    @Column(name = "position", nullable = false)
    private int position;

    @Lob
    @Column(name = "photo", nullable = false)
    private byte[] photo;

    @Column(name = "photo_content_type", nullable = false)
    private String photoContentType;

    protected HouseholdPhoto() {
    }

    public HouseholdPhoto(Household household, int position, byte[] photo, String photoContentType) {
        this.household = household;
        this.position = position;
        this.photo = photo;
        this.photoContentType = photoContentType;
    }

    public UUID getId() {
        return id;
    }

    public Household getHousehold() {
        return household;
    }

    public int getPosition() {
        return position;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }
}
