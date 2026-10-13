package com.onip.facm01.agent.dto;

import com.onip.facm01.agent.AgentRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAgentRequest(
        @NotBlank String username,
        // Même minimum que le formulaire "Créer un compte" du tableau de bord (cf. agents.html)
        // et que PasswordGenerator (mots de passe générés automatiquement, 5 caractères).
        @NotBlank @Size(min = 5, message = "Le mot de passe doit contenir au moins 5 caractères") String password,
        @NotBlank String fullName,
        @NotNull AgentRole role) {
}
