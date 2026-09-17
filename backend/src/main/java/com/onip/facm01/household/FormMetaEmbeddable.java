package com.onip.facm01.household;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class FormMetaEmbeddable {

    @Column(name = "fait_a")
    private String faitA;

    @Column(name = "date_encodage")
    private String dateEncodage;

    @Column(name = "nombre_fiches")
    private String nombreFiches;

    @Column(name = "agent_cartographe")
    private String agentCarthographe;

    private String renseignant;

    protected FormMetaEmbeddable() {
    }

    public FormMetaEmbeddable(String faitA, String dateEncodage, String nombreFiches, String agentCarthographe, String renseignant) {
        this.faitA = faitA;
        this.dateEncodage = dateEncodage;
        this.nombreFiches = nombreFiches;
        this.agentCarthographe = agentCarthographe;
        this.renseignant = renseignant;
    }

    public String getFaitA() {
        return faitA;
    }

    public String getDateEncodage() {
        return dateEncodage;
    }

    public String getNombreFiches() {
        return nombreFiches;
    }

    public String getAgentCarthographe() {
        return agentCarthographe;
    }

    public String getRenseignant() {
        return renseignant;
    }
}
