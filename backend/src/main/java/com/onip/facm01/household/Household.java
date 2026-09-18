package com.onip.facm01.household;

import com.onip.facm01.agent.Agent;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "households")
public class Household {

    @Id
    private UUID id;

    @Column(name = "code_menage", length = 18)
    private String codeMenage;

    @Column(name = "nombre_membres_declare")
    private Integer nombreMembresDeclare;

    @Embedded
    private AddressEmbeddable address;

    @Embedded
    private GeoLocationEmbeddable location;

    @Embedded
    private FormMetaEmbeddable meta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HouseholdStatus status;

    @OneToMany(mappedBy = "household", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<HouseholdMember> members = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "synced_at")
    private Instant syncedAt;

    // Juste un indicateur bon marché ("une photo existe-t-elle ?"), toujours chargé avec le
    // reste de la ligne sans coût. Les octets eux-mêmes vivent dans HouseholdPhoto (table à
    // part) pour ne jamais alourdir les requêtes de liste/recherche avec un BLOB — ils ne sont
    // chargés que sur demande explicite (endpoint dédié).
    // columnDefinition avec DEFAULT : sans ça, la migration auto (ddl-auto=update) d'une colonne
    // NOT NULL sur une table qui a déjà des lignes échoue (rejeté silencieusement par H2 — juste
    // un WARN dans les logs, la colonne n'est alors jamais créée).
    @Column(name = "has_photo", nullable = false, columnDefinition = "boolean default false")
    private boolean hasPhoto = false;

    protected Household() {
    }

    public Household(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getCodeMenage() {
        return codeMenage;
    }

    public void setCodeMenage(String codeMenage) {
        this.codeMenage = codeMenage;
    }

    public Integer getNombreMembresDeclare() {
        return nombreMembresDeclare;
    }

    public void setNombreMembresDeclare(Integer nombreMembresDeclare) {
        this.nombreMembresDeclare = nombreMembresDeclare;
    }

    public AddressEmbeddable getAddress() {
        return address;
    }

    public void setAddress(AddressEmbeddable address) {
        this.address = address;
    }

    public GeoLocationEmbeddable getLocation() {
        return location;
    }

    public void setLocation(GeoLocationEmbeddable location) {
        this.location = location;
    }

    public FormMetaEmbeddable getMeta() {
        return meta;
    }

    public void setMeta(FormMetaEmbeddable meta) {
        this.meta = meta;
    }

    public HouseholdStatus getStatus() {
        return status;
    }

    public void setStatus(HouseholdStatus status) {
        this.status = status;
    }

    public List<HouseholdMember> getMembers() {
        return members;
    }

    public void replaceMembers(List<HouseholdMember> newMembers) {
        this.members.clear();
        for (HouseholdMember member : newMembers) {
            member.setHousehold(this);
            this.members.add(member);
        }
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

    public boolean isHasPhoto() {
        return hasPhoto;
    }

    public void setHasPhoto(boolean hasPhoto) {
        this.hasPhoto = hasPhoto;
    }
}
