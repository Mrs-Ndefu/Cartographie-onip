package com.onip.facm01.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

// Liste du personnel autorisé à avoir un compte, importée par l'ADMIN depuis un fichier Excel
// (nom, email). Sert uniquement à vérifier qu'un compte créé dans "Gérer les agents" correspond
// à quelqu'un de cette liste (cf. AgentService.createAgentFromStaffList) — ce n'est pas une
// gestion des comptes en soi, juste un filtre à la création.
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

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    protected StaffMember() {
    }

    public StaffMember(UUID id, String email, String fullName, Instant importedAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
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

    public Instant getImportedAt() {
        return importedAt;
    }
}
