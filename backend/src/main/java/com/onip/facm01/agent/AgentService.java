package com.onip.facm01.agent;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AgentService {

    private static final long MAX_PHOTO_SIZE = 2 * 1024 * 1024; // 2 Mo

    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;

    public AgentService(AgentRepository agentRepository, PasswordEncoder passwordEncoder) {
        this.agentRepository = agentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Agent createAgent(String username, String rawPassword, String fullName, AgentRole role) {
        if (agentRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Un agent avec ce nom d'utilisateur existe déjà");
        }
        Agent agent = new Agent(UUID.randomUUID(), username, passwordEncoder.encode(rawPassword), fullName, role);
        return agentRepository.save(agent);
    }

    public List<Agent> listAgents() {
        return agentRepository.findAll();
    }

    public void ensureAgentExists(String username, String rawPassword, String fullName, AgentRole role) {
        if (!agentRepository.existsByUsername(username)) {
            createAgent(username, rawPassword, fullName, role);
        }
    }

    public Agent getAgent(UUID id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent introuvable"));
    }

    public Agent setActive(UUID id, boolean active) {
        Agent agent = getAgent(id);
        agent.setActive(active);
        return agentRepository.save(agent);
    }

    public Agent changeRole(UUID id, AgentRole role) {
        Agent agent = getAgent(id);
        agent.setRole(role);
        return agentRepository.save(agent);
    }

    public Agent resetPassword(UUID id, String newPassword) {
        Agent agent = getAgent(id);
        agent.setPasswordHash(passwordEncoder.encode(newPassword));
        return agentRepository.save(agent);
    }

    public Agent changeOwnPassword(UUID id, String currentPassword, String newPassword) {
        Agent agent = getAgent(id);
        if (!passwordEncoder.matches(currentPassword, agent.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        agent.setPasswordHash(passwordEncoder.encode(newPassword));
        return agentRepository.save(agent);
    }

    public Agent updatePhoto(UUID id, byte[] photo, String contentType) {
        if (photo.length > MAX_PHOTO_SIZE) {
            throw new IllegalArgumentException("La photo dépasse la taille maximale autorisée (2 Mo)");
        }
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit être une image");
        }
        Agent agent = getAgent(id);
        agent.setPhoto(photo, contentType);
        return agentRepository.save(agent);
    }
}
