package com.onip.facm01.household.dto;

import com.onip.facm01.household.HouseholdStatus;
import com.onip.facm01.household.Sexe;

import java.util.ArrayList;
import java.util.List;

// Formulaire de modification d'un ménage depuis le tableau de bord (ADMIN, SUPERVISEUR). Classe
// mutable plutôt qu'un record : Spring y lie directement les champs du formulaire, y compris la
// liste indexée des membres (membres[0].nom, ...).
public class HouseholdEditForm {

    private String chefNom;
    private String chefPostnom;
    private String chefPrenom;
    private String chefDateNaissance;
    private Sexe chefSexe;

    private String province;
    private String ville;
    private String commune;
    private String quartier;
    private String rue;
    private String numero;
    private String immeuble;
    private String etage;

    private HouseholdStatus status;

    // Nombre de membres du ménage, chef compris (1 = le chef vit seul). Obligatoire ; on peut
    // ajouter jusqu'à (nombre - 1) fiches membres, sans obligation de les remplir.
    private Integer nombreMembres;

    // Motif obligatoire de la modification, conservé dans l'historique du ménage.
    private String motif;

    private List<MemberForm> membres = new ArrayList<>();

    public static HouseholdEditForm from(HouseholdDto household) {
        HouseholdEditForm form = new HouseholdEditForm();
        if (household.chef() != null) {
            form.chefNom = household.chef().nom();
            form.chefPostnom = household.chef().postnom();
            form.chefPrenom = household.chef().prenom();
            form.chefDateNaissance = household.chef().dateNaissance();
            form.chefSexe = household.chef().sexe();
        }
        if (household.address() != null) {
            form.province = household.address().province();
            form.ville = household.address().ville();
            form.commune = household.address().commune();
            form.quartier = household.address().quartier();
            form.rue = household.address().rue();
            form.numero = household.address().numero();
            form.immeuble = household.address().immeuble();
            form.etage = household.address().etage();
        }
        form.status = household.status();
        form.nombreMembres = household.nombreMembres() != null
                ? household.nombreMembres()
                : 1 + household.membres().size();
        for (PersonDto membre : household.membres()) {
            MemberForm member = new MemberForm();
            member.setNom(membre.nom());
            member.setPostnom(membre.postnom());
            member.setPrenom(membre.prenom());
            member.setDateNaissance(membre.dateNaissance());
            member.setSexe(membre.sexe());
            member.setRelation(membre.relation());
            form.membres.add(member);
        }
        return form;
    }

    public static class MemberForm {
        private String nom;
        private String postnom;
        private String prenom;
        private String dateNaissance;
        private Sexe sexe;
        private String relation;
        // Case "Supprimer" cochée dans le formulaire : le membre n'est pas conservé.
        private boolean removed;

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getPostnom() { return postnom; }
        public void setPostnom(String postnom) { this.postnom = postnom; }
        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }
        public String getDateNaissance() { return dateNaissance; }
        public void setDateNaissance(String dateNaissance) { this.dateNaissance = dateNaissance; }
        public Sexe getSexe() { return sexe; }
        public void setSexe(Sexe sexe) { this.sexe = sexe; }
        public String getRelation() { return relation; }
        public void setRelation(String relation) { this.relation = relation; }
        public boolean isRemoved() { return removed; }
        public void setRemoved(boolean removed) { this.removed = removed; }

        // Ligne ajoutée puis laissée vide : ignorée plutôt qu'enregistrée comme membre sans nom.
        public boolean isBlank() {
            return isEmpty(nom) && isEmpty(postnom) && isEmpty(prenom);
        }

        private static boolean isEmpty(String value) {
            return value == null || value.isBlank();
        }
    }

    public String getChefNom() { return chefNom; }
    public void setChefNom(String chefNom) { this.chefNom = chefNom; }
    public String getChefPostnom() { return chefPostnom; }
    public void setChefPostnom(String chefPostnom) { this.chefPostnom = chefPostnom; }
    public String getChefPrenom() { return chefPrenom; }
    public void setChefPrenom(String chefPrenom) { this.chefPrenom = chefPrenom; }
    public String getChefDateNaissance() { return chefDateNaissance; }
    public void setChefDateNaissance(String chefDateNaissance) { this.chefDateNaissance = chefDateNaissance; }
    public Sexe getChefSexe() { return chefSexe; }
    public void setChefSexe(Sexe chefSexe) { this.chefSexe = chefSexe; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public String getCommune() { return commune; }
    public void setCommune(String commune) { this.commune = commune; }
    public String getQuartier() { return quartier; }
    public void setQuartier(String quartier) { this.quartier = quartier; }
    public String getRue() { return rue; }
    public void setRue(String rue) { this.rue = rue; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getImmeuble() { return immeuble; }
    public void setImmeuble(String immeuble) { this.immeuble = immeuble; }
    public String getEtage() { return etage; }
    public void setEtage(String etage) { this.etage = etage; }
    public HouseholdStatus getStatus() { return status; }
    public void setStatus(HouseholdStatus status) { this.status = status; }
    public Integer getNombreMembres() { return nombreMembres; }
    public void setNombreMembres(Integer nombreMembres) { this.nombreMembres = nombreMembres; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public List<MemberForm> getMembres() { return membres; }
    public void setMembres(List<MemberForm> membres) { this.membres = membres; }
}
