package com.onip.facm01.agent;

import java.util.List;

public enum AgentRole {
    SUPER_ADMIN,
    ADMIN,
    SUPERVISEUR,
    AGENT;

    // Réservé à la gestion des comptes ADMIN/SUPER_ADMIN eux-mêmes (AgentAdminController) : un
    // ADMIN garde tous ses droits sur les comptes AGENT/SUPERVISEUR, mais seul un SUPER_ADMIN
    // peut créer, changer le rôle de, désactiver ou réinitialiser le mot de passe d'un compte
    // admin-tier. Le SUPERVISEUR n'est volontairement pas admin-tier : il ne gère que les AGENT.
    public boolean isAdminTier() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    // Rôles que cet acteur peut assigner (à la création d'un agent, ou en changeant son rôle) —
    // alimente le menu déroulant "Rôle" côté agents.html.
    public List<AgentRole> assignableRoles() {
        return switch (this) {
            case SUPER_ADMIN -> List.of(SUPER_ADMIN, ADMIN, SUPERVISEUR, AGENT);
            case ADMIN -> List.of(SUPERVISEUR, AGENT);
            case SUPERVISEUR -> List.of(AGENT);
            case AGENT -> List.of();
        };
    }

    // Un compte de ce rôle (this = la cible) peut-il être géré (activer/désactiver, changer de
    // rôle, réinitialiser le mot de passe) par un acteur du rôle donné ? Seul un SUPER_ADMIN gère
    // les comptes admin-tier ; un ADMIN gère SUPERVISEUR et AGENT ; un SUPERVISEUR ne gère que
    // les AGENT.
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
