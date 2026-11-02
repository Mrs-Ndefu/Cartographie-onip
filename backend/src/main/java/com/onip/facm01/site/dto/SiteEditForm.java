package com.onip.facm01.site.dto;

import com.onip.facm01.site.AccesRoutier;
import com.onip.facm01.site.AvisGlobal;
import com.onip.facm01.site.ConnectiviteInternet;
import com.onip.facm01.site.EtatBatiment;
import com.onip.facm01.site.OptionOccupation;
import com.onip.facm01.site.ProprieteStatut;
import com.onip.facm01.site.Site;
import com.onip.facm01.site.StabiliteSnel;
import com.onip.facm01.site.TypeInfrastructure;
import com.onip.facm01.site.TypeSite;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// Formulaire web de modification d'une fiche de repérage (réservé à l'ADMIN, cf.
// AgentRole.canEditSites) : les mêmes champs que la fiche papier / l'app tablette, rien
// d'obligatoire (comme à la synchronisation). Les textes vides sont enregistrés comme null.
public class SiteEditForm {
    private String province;
    private String ville;
    private String commune;
    private String quartierGroupement;
    private String villageAvenueRue;
    private String nomSite;
    private String numeroTelephone;
    private String serieTelephoneGps;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double gpsPrecision;
    private TypeSite typeSite;
    private String typeSiteAutre;
    private ProprieteStatut proprieteStatut;
    private OptionOccupation optionOccupation;
    private Boolean disponibiliteImmediate;
    private String delaiEstime;
    private String proprietaireNomContact;
    private TypeInfrastructure typeInfrastructure;
    private String typeInfrastructureAutre;
    private Double superficieBatimentM2;
    private Double superficieTerrainM2;
    private Integer nombrePieces;
    private EtatBatiment etatBatiment;
    private String etatBatimentObservations;
    private AccesRoutier accesRoutier;
    private Boolean raccordeSnel;
    private StabiliteSnel stabiliteSnel;
    private Boolean groupeElectrogeneSolaire;
    private Boolean nonElectrifie;
    private Boolean regideso;
    private Boolean sanitaires;
    private ConnectiviteInternet connectiviteInternet;
    private String reseauxDisponibles;
    private Boolean clotureSecurise;
    private Boolean proximitePolice;
    private Integer distancePoliceMetres;
    private AvisGlobal avisGlobal;
    private String travauxAmenagement;
    private String motifDefavorable;
    private String observationsParticulieres;
    private String nomSignatureEnqueteur;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateEvaluation;

    public static SiteEditForm from(SiteDto s) {
        SiteEditForm f = new SiteEditForm();
        f.province = s.province();
        f.ville = s.ville();
        f.commune = s.commune();
        f.quartierGroupement = s.quartierGroupement();
        f.villageAvenueRue = s.villageAvenueRue();
        f.nomSite = s.nomSite();
        f.numeroTelephone = s.numeroTelephone();
        f.serieTelephoneGps = s.serieTelephoneGps();
        f.latitude = s.latitude();
        f.longitude = s.longitude();
        f.altitude = s.altitude();
        f.gpsPrecision = s.gpsPrecision();
        f.typeSite = s.typeSite();
        f.typeSiteAutre = s.typeSiteAutre();
        f.proprieteStatut = s.proprieteStatut();
        f.optionOccupation = s.optionOccupation();
        f.disponibiliteImmediate = s.disponibiliteImmediate();
        f.delaiEstime = s.delaiEstime();
        f.proprietaireNomContact = s.proprietaireNomContact();
        f.typeInfrastructure = s.typeInfrastructure();
        f.typeInfrastructureAutre = s.typeInfrastructureAutre();
        f.superficieBatimentM2 = s.superficieBatimentM2();
        f.superficieTerrainM2 = s.superficieTerrainM2();
        f.nombrePieces = s.nombrePieces();
        f.etatBatiment = s.etatBatiment();
        f.etatBatimentObservations = s.etatBatimentObservations();
        f.accesRoutier = s.accesRoutier();
        f.raccordeSnel = s.raccordeSnel();
        f.stabiliteSnel = s.stabiliteSnel();
        f.groupeElectrogeneSolaire = s.groupeElectrogeneSolaire();
        f.nonElectrifie = s.nonElectrifie();
        f.regideso = s.regideso();
        f.sanitaires = s.sanitaires();
        f.connectiviteInternet = s.connectiviteInternet();
        f.reseauxDisponibles = s.reseauxDisponibles();
        f.clotureSecurise = s.clotureSecurise();
        f.proximitePolice = s.proximitePolice();
        f.distancePoliceMetres = s.distancePoliceMetres();
        f.avisGlobal = s.avisGlobal();
        f.travauxAmenagement = s.travauxAmenagement();
        f.motifDefavorable = s.motifDefavorable();
        f.observationsParticulieres = s.observationsParticulieres();
        f.nomSignatureEnqueteur = s.nomSignatureEnqueteur();
        f.dateEvaluation = s.dateEvaluation();
        return f;
    }

    public void applyTo(Site site) {
        site.setProvince(blankToNull(province));
        site.setVille(blankToNull(ville));
        site.setCommune(blankToNull(commune));
        site.setQuartierGroupement(blankToNull(quartierGroupement));
        site.setVillageAvenueRue(blankToNull(villageAvenueRue));
        site.setNomSite(blankToNull(nomSite));
        site.setNumeroTelephone(blankToNull(numeroTelephone));
        site.setSerieTelephoneGps(blankToNull(serieTelephoneGps));
        site.setLatitude(latitude);
        site.setLongitude(longitude);
        site.setAltitude(altitude);
        site.setGpsPrecision(gpsPrecision);
        site.setTypeSite(typeSite);
        site.setTypeSiteAutre(blankToNull(typeSiteAutre));
        site.setProprieteStatut(proprieteStatut);
        site.setOptionOccupation(optionOccupation);
        site.setDisponibiliteImmediate(disponibiliteImmediate);
        site.setDelaiEstime(blankToNull(delaiEstime));
        site.setProprietaireNomContact(blankToNull(proprietaireNomContact));
        site.setTypeInfrastructure(typeInfrastructure);
        site.setTypeInfrastructureAutre(blankToNull(typeInfrastructureAutre));
        site.setSuperficieBatimentM2(superficieBatimentM2);
        site.setSuperficieTerrainM2(superficieTerrainM2);
        site.setNombrePieces(nombrePieces);
        site.setEtatBatiment(etatBatiment);
        site.setEtatBatimentObservations(blankToNull(etatBatimentObservations));
        site.setAccesRoutier(accesRoutier);
        site.setRaccordeSnel(raccordeSnel);
        site.setStabiliteSnel(stabiliteSnel);
        site.setGroupeElectrogeneSolaire(groupeElectrogeneSolaire);
        site.setNonElectrifie(nonElectrifie);
        site.setRegideso(regideso);
        site.setSanitaires(sanitaires);
        site.setConnectiviteInternet(connectiviteInternet);
        site.setReseauxDisponibles(blankToNull(reseauxDisponibles));
        site.setClotureSecurise(clotureSecurise);
        site.setProximitePolice(proximitePolice);
        site.setDistancePoliceMetres(distancePoliceMetres);
        site.setAvisGlobal(avisGlobal);
        site.setTravauxAmenagement(blankToNull(travauxAmenagement));
        site.setMotifDefavorable(blankToNull(motifDefavorable));
        site.setObservationsParticulieres(blankToNull(observationsParticulieres));
        site.setNomSignatureEnqueteur(blankToNull(nomSignatureEnqueteur));
        site.setDateEvaluation(dateEvaluation);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getCommune() {
        return commune;
    }

    public void setCommune(String commune) {
        this.commune = commune;
    }

    public String getQuartierGroupement() {
        return quartierGroupement;
    }

    public void setQuartierGroupement(String quartierGroupement) {
        this.quartierGroupement = quartierGroupement;
    }

    public String getVillageAvenueRue() {
        return villageAvenueRue;
    }

    public void setVillageAvenueRue(String villageAvenueRue) {
        this.villageAvenueRue = villageAvenueRue;
    }

    public String getNomSite() {
        return nomSite;
    }

    public void setNomSite(String nomSite) {
        this.nomSite = nomSite;
    }

    public String getNumeroTelephone() {
        return numeroTelephone;
    }

    public void setNumeroTelephone(String numeroTelephone) {
        this.numeroTelephone = numeroTelephone;
    }

    public String getSerieTelephoneGps() {
        return serieTelephoneGps;
    }

    public void setSerieTelephoneGps(String serieTelephoneGps) {
        this.serieTelephoneGps = serieTelephoneGps;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getGpsPrecision() {
        return gpsPrecision;
    }

    public void setGpsPrecision(Double gpsPrecision) {
        this.gpsPrecision = gpsPrecision;
    }

    public TypeSite getTypeSite() {
        return typeSite;
    }

    public void setTypeSite(TypeSite typeSite) {
        this.typeSite = typeSite;
    }

    public String getTypeSiteAutre() {
        return typeSiteAutre;
    }

    public void setTypeSiteAutre(String typeSiteAutre) {
        this.typeSiteAutre = typeSiteAutre;
    }

    public ProprieteStatut getProprieteStatut() {
        return proprieteStatut;
    }

    public void setProprieteStatut(ProprieteStatut proprieteStatut) {
        this.proprieteStatut = proprieteStatut;
    }

    public OptionOccupation getOptionOccupation() {
        return optionOccupation;
    }

    public void setOptionOccupation(OptionOccupation optionOccupation) {
        this.optionOccupation = optionOccupation;
    }

    public Boolean getDisponibiliteImmediate() {
        return disponibiliteImmediate;
    }

    public void setDisponibiliteImmediate(Boolean disponibiliteImmediate) {
        this.disponibiliteImmediate = disponibiliteImmediate;
    }

    public String getDelaiEstime() {
        return delaiEstime;
    }

    public void setDelaiEstime(String delaiEstime) {
        this.delaiEstime = delaiEstime;
    }

    public String getProprietaireNomContact() {
        return proprietaireNomContact;
    }

    public void setProprietaireNomContact(String proprietaireNomContact) {
        this.proprietaireNomContact = proprietaireNomContact;
    }

    public TypeInfrastructure getTypeInfrastructure() {
        return typeInfrastructure;
    }

    public void setTypeInfrastructure(TypeInfrastructure typeInfrastructure) {
        this.typeInfrastructure = typeInfrastructure;
    }

    public String getTypeInfrastructureAutre() {
        return typeInfrastructureAutre;
    }

    public void setTypeInfrastructureAutre(String typeInfrastructureAutre) {
        this.typeInfrastructureAutre = typeInfrastructureAutre;
    }

    public Double getSuperficieBatimentM2() {
        return superficieBatimentM2;
    }

    public void setSuperficieBatimentM2(Double superficieBatimentM2) {
        this.superficieBatimentM2 = superficieBatimentM2;
    }

    public Double getSuperficieTerrainM2() {
        return superficieTerrainM2;
    }

    public void setSuperficieTerrainM2(Double superficieTerrainM2) {
        this.superficieTerrainM2 = superficieTerrainM2;
    }

    public Integer getNombrePieces() {
        return nombrePieces;
    }

    public void setNombrePieces(Integer nombrePieces) {
        this.nombrePieces = nombrePieces;
    }

    public EtatBatiment getEtatBatiment() {
        return etatBatiment;
    }

    public void setEtatBatiment(EtatBatiment etatBatiment) {
        this.etatBatiment = etatBatiment;
    }

    public String getEtatBatimentObservations() {
        return etatBatimentObservations;
    }

    public void setEtatBatimentObservations(String etatBatimentObservations) {
        this.etatBatimentObservations = etatBatimentObservations;
    }

    public AccesRoutier getAccesRoutier() {
        return accesRoutier;
    }

    public void setAccesRoutier(AccesRoutier accesRoutier) {
        this.accesRoutier = accesRoutier;
    }

    public Boolean getRaccordeSnel() {
        return raccordeSnel;
    }

    public void setRaccordeSnel(Boolean raccordeSnel) {
        this.raccordeSnel = raccordeSnel;
    }

    public StabiliteSnel getStabiliteSnel() {
        return stabiliteSnel;
    }

    public void setStabiliteSnel(StabiliteSnel stabiliteSnel) {
        this.stabiliteSnel = stabiliteSnel;
    }

    public Boolean getGroupeElectrogeneSolaire() {
        return groupeElectrogeneSolaire;
    }

    public void setGroupeElectrogeneSolaire(Boolean groupeElectrogeneSolaire) {
        this.groupeElectrogeneSolaire = groupeElectrogeneSolaire;
    }

    public Boolean getNonElectrifie() {
        return nonElectrifie;
    }

    public void setNonElectrifie(Boolean nonElectrifie) {
        this.nonElectrifie = nonElectrifie;
    }

    public Boolean getRegideso() {
        return regideso;
    }

    public void setRegideso(Boolean regideso) {
        this.regideso = regideso;
    }

    public Boolean getSanitaires() {
        return sanitaires;
    }

    public void setSanitaires(Boolean sanitaires) {
        this.sanitaires = sanitaires;
    }

    public ConnectiviteInternet getConnectiviteInternet() {
        return connectiviteInternet;
    }

    public void setConnectiviteInternet(ConnectiviteInternet connectiviteInternet) {
        this.connectiviteInternet = connectiviteInternet;
    }

    public String getReseauxDisponibles() {
        return reseauxDisponibles;
    }

    public void setReseauxDisponibles(String reseauxDisponibles) {
        this.reseauxDisponibles = reseauxDisponibles;
    }

    public Boolean getClotureSecurise() {
        return clotureSecurise;
    }

    public void setClotureSecurise(Boolean clotureSecurise) {
        this.clotureSecurise = clotureSecurise;
    }

    public Boolean getProximitePolice() {
        return proximitePolice;
    }

    public void setProximitePolice(Boolean proximitePolice) {
        this.proximitePolice = proximitePolice;
    }

    public Integer getDistancePoliceMetres() {
        return distancePoliceMetres;
    }

    public void setDistancePoliceMetres(Integer distancePoliceMetres) {
        this.distancePoliceMetres = distancePoliceMetres;
    }

    public AvisGlobal getAvisGlobal() {
        return avisGlobal;
    }

    public void setAvisGlobal(AvisGlobal avisGlobal) {
        this.avisGlobal = avisGlobal;
    }

    public String getTravauxAmenagement() {
        return travauxAmenagement;
    }

    public void setTravauxAmenagement(String travauxAmenagement) {
        this.travauxAmenagement = travauxAmenagement;
    }

    public String getMotifDefavorable() {
        return motifDefavorable;
    }

    public void setMotifDefavorable(String motifDefavorable) {
        this.motifDefavorable = motifDefavorable;
    }

    public String getObservationsParticulieres() {
        return observationsParticulieres;
    }

    public void setObservationsParticulieres(String observationsParticulieres) {
        this.observationsParticulieres = observationsParticulieres;
    }

    public String getNomSignatureEnqueteur() {
        return nomSignatureEnqueteur;
    }

    public void setNomSignatureEnqueteur(String nomSignatureEnqueteur) {
        this.nomSignatureEnqueteur = nomSignatureEnqueteur;
    }

    public LocalDate getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(LocalDate dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }
}
