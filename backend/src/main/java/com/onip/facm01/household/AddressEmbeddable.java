package com.onip.facm01.household;

import jakarta.persistence.Embeddable;

@Embeddable
public class AddressEmbeddable {

    private String ville;
    private String commune;
    private String quartier;
    private String rue;
    private String numero;
    private String immeuble;
    private String etage;

    protected AddressEmbeddable() {
    }

    public AddressEmbeddable(
            String ville, String commune, String quartier, String rue, String numero, String immeuble, String etage) {
        this.ville = ville;
        this.commune = commune;
        this.quartier = quartier;
        this.rue = rue;
        this.numero = numero;
        this.immeuble = immeuble;
        this.etage = etage;
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

    public String getRue() {
        return rue;
    }

    public String getNumero() {
        return numero;
    }

    public String getImmeuble() {
        return immeuble;
    }

    public String getEtage() {
        return etage;
    }
}
