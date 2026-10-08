package com.onip.facm01.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

// Liste du personnel autorisé à avoir un compte, importée par l'ADMIN depuis un fichier Excel
// (nom, email, rôle optionnel). Sert à vérifier qu'un compte créé dans "Gérer les agents"
// correspond à quelqu'un de cette liste (cf. AgentService.createAgentFromStaffList) et à
// pré-remplir le formulaire (nom, email, rôle suggéré — cf. agents.html) — ce n'est pas une
// gestion des comptes en soi, juste un filtre et une aide à la saisie.
@Entity
@Table(name = "staff_members")
public class StaffMember {

    @Id
    private UUID id;

    // Comparé sans tenir compte de la casse (cf. StaffMemberRepository) — stocké tel quel pour
    // l'affichage.
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    // Rôle suggéré par le fichier Excel (colonne "Rôle", optionnelle — null si absente ou non
    // reconnue). Purement indicatif : seul pré-remplit le formulaire de création, n'est pas
    // imposé (cf. AgentService.createAgentFromStaffList, qui ne vérifie que nom + email).
    @Enumerated(EnumType.STRING)
    @Column
    private AgentRole role;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    protected StaffMember() {
    }

    public StaffMember(UUID id, String email, String fullName, AgentRole role, Instant importedAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.importedAt = importedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public AgentRole getRole() {
        return role;
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}
