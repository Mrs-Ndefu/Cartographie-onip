package com.onip.facm01.site;

import com.onip.facm01.agent.Agent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Fiche de repérage d'un site candidat (bâtiment ou terrain) pour un bureau ONIP — remplace
// l'ancien "repérage" (simple marqueur GPS local, jamais synchronisé). Reprend un à un les champs
// de FICHE REPERAGE DES SITES.docx (fournie par l'utilisateur) : rien n'est obligatoire au niveau
// base/colonne, l'agent peut synchroniser une fiche partiellement remplie et la compléter plus
// tard (même principe que Household).
@Entity
@Table(name = "sites")
public class Site {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "synced_at")
    private Instant syncedAt;

    // I. Identification administrative du site
    private String province;
    private String ville;
    private String commune;
    @Column(name = "quartier_groupement")
    private String quartierGroupement;
    @Column(name = "village_avenue_rue")
    private String villageAvenueRue;
    @Column(name = "nom_site")
    private String nomSite;
    @Column(name = "numero_telephone")
    private String numeroTelephone;
    @Column(name = "serie_telephone_gps")
    private String serieTelephoneGps;

    // II. Coordonnées géographiques (GPS)
    private Double latitude;
    private Double longitude;
    private Double altitude;
    @Column(name = "gps_precision")
    private Double gpsPrecision;

    // III. Type de site
    @Enumerated(EnumType.STRING)
    @Column(name = "type_site")
    private TypeSite typeSite;
    @Column(name = "type_site_autre")
    private String typeSiteAutre;

    // IV. Statut juridique et propriété
    @Enumerated(EnumType.STRING)
    @Column(name = "propriete_statut")
    private ProprieteStatut proprieteStatut;
    @Enumerated(EnumType.STRING)
    @Column(name = "option_occupation")
    private OptionOccupation optionOccupation;
    @Column(name = "disponibilite_immediate")
    private Boolean disponibiliteImmediate;
    @Column(name = "delai_estime")
    private String delaiEstime;
    @Column(name = "proprietaire_nom_contact")
    private String proprietaireNomContact;

    // V. Caractéristiques physiques et infrastructures
    @Enumerated(EnumType.STRING)
    @Column(name = "type_infrastructure")
    private TypeInfrastructure typeInfrastructure;
    @Column(name = "type_infrastructure_autre")
    private String typeInfrastructureAutre;
    @Column(name = "superficie_batiment_m2")
    private Double superficieBatimentM2;
    @Column(name = "superficie_terrain_m2")
    private Double superficieTerrainM2;
    @Column(name = "nombre_pieces")
    private Integer nombrePieces;
    @Enumerated(EnumType.STRING)
    @Column(name = "etat_batiment")
    private EtatBatiment etatBatiment;
    @Column(name = "etat_batiment_observations")
    private String etatBatimentObservations;

    // VI. Commodité et accès techniques
    @Enumerated(EnumType.STRING)
    @Column(name = "acces_routier")
    private AccesRoutier accesRoutier;
    @Column(name = "raccorde_snel")
    private Boolean raccordeSnel;
    @Enumerated(EnumType.STRING)
    @Column(name = "stabilite_snel")
    private StabiliteSnel stabiliteSnel;
    @Column(name = "groupe_electrogene_solaire")
    private Boolean groupeElectrogeneSolaire;
    @Column(name = "non_electrifie")
    private Boolean nonElectrifie;
    private Boolean regideso;
    private Boolean sanitaires;
    @Enumerated(EnumType.STRING)
    @Column(name = "connectivite_internet")
    private ConnectiviteInternet connectiviteInternet;
    @Column(name = "reseaux_disponibles")
    private String reseauxDisponibles;
    @Column(name = "cloture_securise")
    private Boolean clotureSecurise;
    @Column(name = "proximite_police")
    private Boolean proximitePolice;
    @Column(name = "distance_police_metres")
    private Integer distancePoliceMetres;

    // VII. Évaluation et recommandations de l'équipe de terrain
    @Enumerated(EnumType.STRING)
    @Column(name = "avis_global")
    private AvisGlobal avisGlobal;
    @Column(name = "travaux_amenagement")
    private String travauxAmenagement;
    @Column(name = "motif_defavorable")
    private String motifDefavorable;
    @Column(name = "observations_particulieres")
    private String observationsParticulieres;
    @Column(name = "nom_signature_enqueteur")
    private String nomSignatureEnqueteur;
    @Column(name = "date_evaluation")
    private LocalDate dateEvaluation;

    protected Site() {
    }

    public Site(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
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
