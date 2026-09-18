package com.onip.facm01.household;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.util.UUID;

// Table séparée de Household exprès : ses octets ne doivent être chargés que sur demande
// explicite (un ménage à la fois), jamais entraînés par une requête de liste/recherche portant
// sur Household. Voir le commentaire sur Household.hasPhoto.
@Entity
@Table(name = "household_photos")
public class HouseholdPhoto {

    @Id
    @Column(name = "household_id")
    private UUID householdId;

    @Lob
    @Column(name = "photo", nullable = false)
    private byte[] photo;

    @Column(name = "photo_content_type", nullable = false)
    private String photoContentType;

    protected HouseholdPhoto() {
    }

    public HouseholdPhoto(UUID householdId, byte[] photo, String photoContentType) {
        this.householdId = householdId;
        this.photo = photo;
        this.photoContentType = photoContentType;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void update(byte[] photo, String photoContentType) {
        this.photo = photo;
        this.photoContentType = photoContentType;
    }
}
