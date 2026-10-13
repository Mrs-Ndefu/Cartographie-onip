package com.onip.facm01.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        // Même minimum que PasswordGenerator (mots de passe générés automatiquement, 5
        // caractères) — cf. AgentAdminController#resetPassword, même règle côté tableau de bord.
        @NotBlank @Size(min = 5, message = "Le mot de passe doit contenir au moins 5 caractères") String newPassword) {
}
