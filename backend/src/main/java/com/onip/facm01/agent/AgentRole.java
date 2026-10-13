package com.onip.facm01.agent;

import com.onip.facm01.household.HouseholdStatus;

import java.util.List;

public enum AgentRole {
    SUPER_ADMIN,
    ADMIN,
    SUPERVISEUR,
    // Rôle intermédiaire entre le superviseur et l'agent : le superviseur crée un chef d'équipe
    // et lui affecte des agents (cf. Agent.superviseur, réutilisé pour ce niveau aussi — un
    // agent est affecté à un chef d'équipe, un chef d'équipe à un superviseur). Remplace le lien
    // direct superviseur→agent : un superviseur n'affecte plus d'agents à lui-même, seulement à
    // un chef d'équipe. Rôle purement structurel — pas de pouvoir sur les ménages (valider/
    // rejeter, historique restent réservés au superviseur), juste la gestion de son équipe
    // (créer un compte AGENT, activer/désactiver, réinitialiser son mot de passe — cf.
    // canBeManagedBy).
    CHEF_EQUIPE,
    DIRECTION_GENERALE,
    AGENT;

    // Libellé affiché dans le tableau de bord.
    public String label() {
        return switch (this) {
            case SUPER_ADMIN -> "Super admin";
            case ADMIN -> "Admin";
            case SUPERVISEUR -> "Superviseur";
            case CHEF_EQUIPE -> "Chef d'équipe";
            case DIRECTION_GENERALE -> "Direction générale";
            case AGENT -> "Agent";
        };
    }

    // Comptes affectés à une zone de terrain : l'agent (zone où il enregistre) et le superviseur
    // (zone qu'il encadre). Le chef d'équipe n'en a pas (rôle structurel, pas de terrain propre).
    public boolean canHaveZone() {
        return this == AGENT || this == SUPERVISEUR;
    }

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
    // page de statistiques ; le CHEF_EQUIPE n'y a pas accès du tout (rôle structurel, cf. plus
    // haut) — sa seule page est "Gérer les agents" (cf. AgentRole.homePath, SecurityConfig).
    public boolean canViewHouseholds() {
        return this == SUPER_ADMIN || this == ADMIN || this == SUPERVISEUR;
    }

    // Modifier les champs d'un ménage (formulaire d'édition) : réservé à l'ADMIN et au
    // SUPER_ADMIN. Le SUPERVISEUR ne modifie plus — il valide ou rejette (cf.
    // canReviewHousehold), ce qui est différent de changer les champs saisis par l'agent.
    public boolean canEditHousehold(HouseholdStatus status) {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    // Valider ou rejeter un ménage incomplet (pas en modifier les champs) : l'ADMIN peut agir sur
    // n'importe quel ménage, le SUPERVISEUR seulement sur les siens qui ne sont pas encore
    // complets — cf. household-detail.html (page détail, seul endroit où ces actions existent).
    // Le CHEF_EQUIPE n'a volontairement pas ce pouvoir (rôle structurel uniquement).
    public boolean canReviewHousehold(HouseholdStatus status) {
        return this == ADMIN || (this == SUPERVISEUR && status != HouseholdStatus.COMPLET);
    }

    public boolean canRemoveHouseholds() {
        return this == ADMIN;
    }

    // Restaurer un ménage retiré : l'ADMIN, et aussi le SUPERVISEUR.
    public boolean canRestoreHouseholds() {
        return this == ADMIN || this == SUPERVISEUR;
    }

    // Affecter un compte à son encadrant (agent→chef d'équipe, chef d'équipe→superviseur) depuis
    // le tableau "Comptes" (menu déroulant "Encadrant", cf. agents.html) : réservé aux comptes
    // admin-tier. Un SUPERVISEUR/CHEF_EQUIPE qui crée un compte se l'affecte automatiquement
    // (cf. AgentAdminController#create), mais ne peut pas réaffecter un compte existant ici.
    public boolean canAssignSupervisors() {
        return isAdminTier();
    }

    // Les zones d'affectation sont définies à l'avance par l'ADMIN ; SUPER_ADMIN et SUPERVISEUR
    // peuvent seulement les consulter (et le SUPERVISEUR y affecte les agents).
    public boolean canManageZones() {
        return this == ADMIN;
    }

    // Historique des enregistrements de ses agents, dans ou hors de leur zone : SUPERVISEUR seul.
    public boolean canViewSupervisorHistory() {
        return this == SUPERVISEUR;
    }

    public boolean canAccessAgents() {
        return this == SUPER_ADMIN || this == ADMIN || this == SUPERVISEUR;
    }

    // Page d'accueil après connexion au tableau de bord web (cf. SecurityConfig#dashboardFilterChain,
    // qui applique la même logique dans son successHandler — ce booléen y sert de référence).
    public String homePath() {
        if (this == DIRECTION_GENERALE) {
            return "/dashboard/stats";
        }
        if (this == CHEF_EQUIPE) {
            return "/dashboard/agents";
        }
        return "/dashboard";
    }

    // Rôles que cet acteur peut assigner (à la création d'un agent, ou en changeant son rôle) —
    // alimente le menu déroulant "Rôle" côté agents.html. Un SUPERVISEUR crée désormais des
    // CHEF_EQUIPE (plus directement des AGENT) ; un CHEF_EQUIPE crée des AGENT.
    public List<AgentRole> assignableRoles() {
        return switch (this) {
            case SUPER_ADMIN -> List.of(SUPER_ADMIN, ADMIN, SUPERVISEUR, CHEF_EQUIPE, DIRECTION_GENERALE, AGENT);
            case ADMIN -> List.of(SUPERVISEUR, CHEF_EQUIPE, DIRECTION_GENERALE, AGENT);
            case SUPERVISEUR -> List.of(CHEF_EQUIPE);
            case CHEF_EQUIPE -> List.of(AGENT);
            case DIRECTION_GENERALE, AGENT -> List.of();
        };
    }

    // Un compte de ce rôle (this = la cible) peut-il être géré (activer/désactiver, changer de
    // rôle, réinitialiser le mot de passe, affecter à une zone) par un acteur du rôle donné ?
    // Seul un SUPER_ADMIN gère les comptes admin-tier ; un ADMIN gère tous les autres ; un
    // SUPERVISEUR gère uniquement ses CHEF_EQUIPE, un CHEF_EQUIPE uniquement ses AGENT (cf.
    // AgentAdminController#requireCanManage, qui vérifie en plus l'affectation réelle).
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
        if (actorRole == SUPERVISEUR) {
            return this == CHEF_EQUIPE;
        }
        return actorRole == CHEF_EQUIPE && this == AGENT;
    }
}
