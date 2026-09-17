package com.onip.facm01.agent;

import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.agent.dto.ChangePasswordRequest;
import com.onip.facm01.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Endpoints en libre-service pour l'agent connecté (tout rôle), à distinguer de
 * AgentController qui est réservé aux ADMIN pour gérer les AUTRES agents.
 */
@RestController
@RequestMapping("/api/me")
public class AgentSelfController {

    private final AgentService agentService;
    private final AgentRepository agentRepository;

    public AgentSelfController(AgentService agentService, AgentRepository agentRepository) {
        this.agentService = agentService;
        this.agentRepository = agentRepository;
    }

    @GetMapping
    public AgentDto me(HttpServletRequest request) {
        return AgentDto.from(currentAgent(request));
    }

    @PutMapping("/password")
    public AgentDto changePassword(@Valid @RequestBody ChangePasswordRequest req, HttpServletRequest request) {
        Agent agent = currentAgent(request);
        return AgentDto.from(agentService.changeOwnPassword(agent.getId(), req.currentPassword(), req.newPassword()));
    }

    @PostMapping("/photo")
    public AgentDto uploadPhoto(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Agent agent = currentAgent(request);
        try {
            agentService.updatePhoto(agent.getId(), file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le fichier envoyé", e);
        }
        return AgentDto.from(agentService.getAgent(agent.getId()));
    }

    private Agent currentAgent(HttpServletRequest request) {
        Object agentId = request.getAttribute(JwtAuthFilter.AGENT_ID_ATTRIBUTE);
        if (agentId == null) {
            throw new IllegalStateException("Non authentifié");
        }
        return agentRepository.findById(UUID.fromString(agentId.toString()))
                .orElseThrow(() -> new IllegalArgumentException("Agent introuvable"));
    }
}
