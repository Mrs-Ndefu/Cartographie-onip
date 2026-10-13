package com.onip.facm01.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentRoleTest {

    @Test
    void superviseurOnlyAssignsChefEquipe() {
        assertTrue(AgentRole.SUPERVISEUR.assignableRoles().contains(AgentRole.CHEF_EQUIPE));
        assertFalse(AgentRole.SUPERVISEUR.assignableRoles().contains(AgentRole.AGENT));
    }

    @Test
    void chefEquipeOnlyAssignsAgent() {
        assertTrue(AgentRole.CHEF_EQUIPE.assignableRoles().contains(AgentRole.AGENT));
        assertFalse(AgentRole.CHEF_EQUIPE.assignableRoles().contains(AgentRole.CHEF_EQUIPE));
    }

    @Test
    void chefEquipeManagesOnlyAgent() {
        assertTrue(AgentRole.AGENT.canBeManagedBy(AgentRole.CHEF_EQUIPE));
        assertFalse(AgentRole.CHEF_EQUIPE.canBeManagedBy(AgentRole.CHEF_EQUIPE));
        assertFalse(AgentRole.SUPERVISEUR.canBeManagedBy(AgentRole.CHEF_EQUIPE));
    }

    @Test
    void superviseurManagesOnlyChefEquipeNotAgentDirectly() {
        assertTrue(AgentRole.CHEF_EQUIPE.canBeManagedBy(AgentRole.SUPERVISEUR));
        assertFalse(AgentRole.AGENT.canBeManagedBy(AgentRole.SUPERVISEUR));
    }

    @Test
    void adminTierStillManagesEveryoneBelow() {
        assertTrue(AgentRole.CHEF_EQUIPE.canBeManagedBy(AgentRole.ADMIN));
        assertTrue(AgentRole.AGENT.canBeManagedBy(AgentRole.ADMIN));
        assertTrue(AgentRole.CHEF_EQUIPE.canBeManagedBy(AgentRole.SUPER_ADMIN));
    }

    @Test
    void chefEquipeHasNoHouseholdReviewPowers() {
        assertFalse(AgentRole.CHEF_EQUIPE.canViewHouseholds());
        assertFalse(AgentRole.CHEF_EQUIPE.canReviewHousehold(com.onip.facm01.household.HouseholdStatus.BROUILLON));
        assertFalse(AgentRole.CHEF_EQUIPE.canViewSupervisorHistory());
    }

    @Test
    void chefEquipeHomePathIsAgentsPage() {
        assertTrue(AgentRole.CHEF_EQUIPE.homePath().equals("/dashboard/agents"));
    }
}
