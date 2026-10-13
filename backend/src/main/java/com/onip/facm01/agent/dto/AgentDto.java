package com.onip.facm01.agent.dto;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRole;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public record AgentDto(
        UUID id,
        String username,
        String fullName,
        AgentRole role,
        boolean active,
        Instant createdAt,
        String photoDataUrl,
        UUID zoneId,
        String zoneCode,
        String zoneLabel,
        String zonePlace,
        // Repris séparément de zonePlace (qui les concatène pour l'affichage) : l'app Android en
        // a besoin sous cette forme pour restreindre les listes province/ville/commune de la
        // saisie terrain à la zone de l'agent (cf. CaptureScreen côté Android).
        String zoneProvince,
        String zoneVille,
        List<String> zoneCommunes,
        UUID superviseurId,
        String superviseurName,
        // Jamais rempli par from(Agent) ci-dessous (toujours null) — seule
        // withGeneratedPassword(Agent) le renseigne. Ce DTO est renvoyé par l'API REST (login,
        // /me...) et pour afficher sa PROPRE fiche (profil, topbar) ; un mot de passe en clair
        // n'a rien à faire dans ces réponses-là. Ne circule que dans "Gérer" (cf.
        // AgentAdminController.list, agents.html), la seule page où un ADMIN consulte le dernier
        // mot de passe généré pour un AUTRE compte.
        String generatedPassword) {

    public static AgentDto from(Agent agent) {
        return build(agent, null);
    }

    public static AgentDto withGeneratedPassword(Agent agent) {
        return build(agent, agent.getGeneratedPassword());
    }

    private static AgentDto build(Agent agent, String generatedPassword) {
        String photoDataUrl = null;
        if (agent.getPhoto() != null && agent.getPhoto().length > 0 && agent.getPhotoContentType() != null) {
            photoDataUrl = "data:" + agent.getPhotoContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(agent.getPhoto());
        }
        return new AgentDto(
                agent.getId(),
                agent.getUsername(),
                agent.getFullName(),
                agent.getRole(),
                agent.isActive(),
                agent.getCreatedAt(),
                photoDataUrl,
                agent.getZone() == null ? null : agent.getZone().getId(),
                agent.getZone() == null ? null : agent.getZone().getCode(),
                agent.getZone() == null ? null : agent.getZone().getLabel(),
                agent.getZone() == null ? null : agent.getZone().getPlace(),
                agent.getZone() == null ? null : agent.getZone().getProvince(),
                agent.getZone() == null ? null : agent.getZone().getVille(),
                agent.getZone() == null ? null : agent.getZone().getCommunes(),
                agent.getSuperviseur() == null ? null : agent.getSuperviseur().getId(),
                agent.getSuperviseur() == null ? null : agent.getSuperviseur().getFullName(),
                generatedPassword);
    }
}
