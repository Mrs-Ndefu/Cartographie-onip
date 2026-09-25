package com.onip.facm01.household;

import com.onip.facm01.agent.AgentRole;
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
import java.util.UUID;

// Trace d'une modification faite depuis le tableau de bord (qui, quand, pourquoi) — affichée
// dans le détail du ménage. L'auteur est recopié (nom, rôle) plutôt que référencé : la trace doit
// rester lisible même si le compte change de rôle ou est supprimé plus tard.
@Entity
@Table(name = "household_modifications")
public class HouseholdModification {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "author_username", nullable = false)
    private String authorUsername;

    @Column(name = "author_full_name")
    private String authorFullName;

    // Chaîne plutôt qu'@Enumerated(STRING) : sur H2, Hibernate crée une colonne ENUM figée sur
    // les valeurs du moment (cf. RoleColumnMigration pour agents.role).
    @Column(name = "author_role", nullable = false, length = 32)
    private String authorRole;

    @Column(nullable = false, length = 1000)
    private String motif;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    protected HouseholdModification() {
    }

    public HouseholdModification(
            Household household, String authorUsername, String authorFullName, AgentRole authorRole, String motif) {
        this.id = UUID.randomUUID();
        this.household = household;
        this.authorUsername = authorUsername;
        this.authorFullName = authorFullName;
        this.authorRole = authorRole.name();
        this.motif = motif;
        this.modifiedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public String getAuthorFullName() {
        return authorFullName;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public boolean isBySuperviseur() {
        return AgentRole.SUPERVISEUR.name().equals(authorRole);
    }

    public String getMotif() {
        return motif;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }
}
