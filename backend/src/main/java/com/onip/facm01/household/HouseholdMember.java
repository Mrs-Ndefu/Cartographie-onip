package com.onip.facm01.household;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "household_members")
public class HouseholdMember {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    /** true pour le chef de ménage, false pour un membre. */
    @Column(nullable = false)
    private boolean chef;

    /** Ordre d'affichage : -1 pour le chef, 0..13 pour les membres. */
    @Column(nullable = false)
    private int position;

    private String nom;
    private String postnom;
    private String prenom;

    @Column(name = "date_naissance")
    private String dateNaissance;

    @Enumerated(EnumType.STRING)
    private Sexe sexe;

    /** Lien de parenté avec le chef de ménage (vide pour le chef lui-même). */
    private String relation;

    protected HouseholdMember() {
    }

    public HouseholdMember(UUID id, Household household, boolean chef, int position,
                            String nom, String postnom, String prenom, String dateNaissance, Sexe sexe, String relation) {
        this.id = id;
        this.household = household;
        this.chef = chef;
        this.position = position;
        this.nom = nom;
        this.postnom = postnom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.sexe = sexe;
        this.relation = relation;
    }

    public UUID getId() {
        return id;
    }

    public Household getHousehold() {
        return household;
    }

    public void setHousehold(Household household) {
        this.household = household;
    }

    public boolean isChef() {
        return chef;
    }

    public int getPosition() {
        return position;
    }

    public String getNom() {
        return nom;
    }

    public String getPostnom() {
        return postnom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getDateNaissance() {
        return dateNaissance;
    }

    public Sexe getSexe() {
        return sexe;
    }

    public String getRelation() {
        return relation;
    }
}
