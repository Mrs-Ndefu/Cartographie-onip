package com.onip.facm01.household;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class GeoLocationEmbeddable {

    private Double latitude;
    private Double longitude;

    @Column(name = "gps_precision")
    private Double precision;

    @Column(name = "saisie_manuelle")
    private Boolean saisieManuelle;

    protected GeoLocationEmbeddable() {
    }

    public GeoLocationEmbeddable(Double latitude, Double longitude, Double precision, Boolean saisieManuelle) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.precision = precision;
        this.saisieManuelle = saisieManuelle;
    }

    public boolean isPresent() {
        return latitude != null && longitude != null;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getPrecision() {
        return precision;
    }

    public Boolean getSaisieManuelle() {
        return saisieManuelle;
    }
}
