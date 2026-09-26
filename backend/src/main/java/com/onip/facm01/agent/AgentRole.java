package com.onip.facm01.agent;

import com.onip.facm01.household.HouseholdStatus;

import java.util.List;

public enum AgentRole {
    SUPER_ADMIN,
    ADMIN,
    SUPERVISEUR,
    DIRECTION_GENERALE,
    AGENT;

    // Réservé à la gestion des comptes ADMIN/SUPER_ADMIN eux-mêmes (AgentAdminController) : un
    // ADMIN garde tous ses droits sur les comptes SUPERVISEUR/DIRECTION_GENERALE/AGENT, mais seul
    // un SUPER_ADMIN peut créer, changer le rôle de, désactiver ou réinitialiser le mot de passe
    // d'un compte admin-tier. Sert aussi, côté API REST, à laisser un compte voir tous les ménages
    // plutôt que seulement les siens (HouseholdService).
    public boolean isAdminTier() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    // Tableau de bord des ménages (liste, carte, détail, ménages retirés). Le SUPER_ADMIN voit
    // tout mais ne pose aucune action sur les ménages ; la DIRECTION_GENERALE n'a accès qu'à la
    // page de statistiques.
    public boolean canViewHouseholds() {
        return this == SUPER_ADMIN || this == ADMIN || this == SUPERVISEUR;
    }

    public boolean canEditHouseholds() {
        return this == ADMIN || this == SUPERVISEUR;
    }

    // Le SUPERVISEUR ne modifie que les ménages incomplets ; l'ADMIN modifie tout ménage.
    public boolean canEditHousehold(HouseholdStatus status) {
        return this == ADMIN || (this == SUPERVISEUR && status != HouseholdStatus.COMPLET);
    }

    public boolean canRemoveHouseholds() {
        return this == ADMIN;
    }

    // Restaurer un ménage retiré : l'ADMIN, et aussi le SUPERVISEUR.
    public boolean canRestoreHouseholds() {
        return this == ADMIN || this == SUPERVISEUR;
    }

    // Affecter un agent à un superviseur : réservé aux comptes admin-tier.
    public boolean canAssignSupervisors() {
        return isAdminTier();
    }

    // Les zones d'affectation sont définies à l'avance par l'ADMIN ; SUPER_ADMIN et SUPERVISEUR
    // peuvent seulement les consulter (et le SUPERVISEUR y affecte les agents).
    public boolean canManageZones() {
        return this == ADMIN;
    }

    public boolean canAccessAgents() {
        return this == SUPER_ADMIN || this == ADMIN || this == SUPERVISEUR;
    }

    // Page d'accueil après connexion au tableau de bord web.
    public String homePath() {
        return this == DIRECTION_GENERALE ? "/dashboard/stats" : "/dashboard";
    }

    // Rôles que cet acteur peut assigner (à la création d'un agent, ou en changeant son rôle) —
    // alimente le menu déroulant "Rôle" côté agents.html.
    public List<AgentRole> assignableRoles() {
        return switch (this) {
            case SUPER_ADMIN -> List.of(SUPER_ADMIN, ADMIN, SUPERVISEUR, DIRECTION_GENERALE, AGENT);
            case ADMIN -> List.of(SUPERVISEUR, DIRECTION_GENERALE, AGENT);
            case SUPERVISEUR -> List.of(AGENT);
            case DIRECTION_GENERALE, AGENT -> List.of();
        };
    }

    // Un compte de ce rôle (this = la cible) peut-il être géré (activer/désactiver, changer de
    // rôle, réinitialiser le mot de passe, affecter à une zone) par un acteur du rôle donné ?
    // Seul un SUPER_ADMIN gère les comptes admin-tier ; un ADMIN gère tous les autres ; un
    // SUPERVISEUR gère uniquement les AGENT.
    public boolean canBeManagedBy(AgentRole actorRole) {
        if (actorRole == SUPER_ADMIN) {
            return true;
        }
        if (this.isAdminTier()) {
            return false;
        }
        if (actorRole == ADMIN) {
            return true;
        }
        return actorRole == SUPERVISEUR && this == AGENT;
    }
}
