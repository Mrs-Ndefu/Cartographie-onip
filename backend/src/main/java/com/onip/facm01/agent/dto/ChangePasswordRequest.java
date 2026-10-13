package com.onip.facm01.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        // Même minimum que la création/réinitialisation d'un compte (cf. PasswordGenerator,
        // CreateAgentRequest, ResetPasswordRequest) : un agent doit pouvoir reprendre un mot de
        // passe de la même longueur que celui généré automatiquement pour lui.
        @NotBlank @Size(min = 5, message = "Le mot de passe doit contenir au moins 5 caractères") String newPassword) {
}
