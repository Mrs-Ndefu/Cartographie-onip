package com.onip.facm01.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères") String newPassword) {
}
