package com.onip.facm01.agent.dto;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRole;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record AgentDto(
        UUID id,
        String username,
        String fullName,
        AgentRole role,
        boolean active,
        Instant createdAt,
        String photoDataUrl) {

    public static AgentDto from(Agent agent) {
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
                photoDataUrl);
    }
}
