package com.onip.facm01.agent.dto;

import com.onip.facm01.agent.AgentRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAgentRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères") String password,
        @NotBlank String fullName,
        @NotNull AgentRole role) {
}
