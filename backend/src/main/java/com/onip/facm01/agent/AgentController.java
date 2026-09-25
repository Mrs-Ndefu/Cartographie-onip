package com.onip.facm01.agent;

import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.agent.dto.ChangeRoleRequest;
import com.onip.facm01.agent.dto.CreateAgentRequest;
import com.onip.facm01.agent.dto.ResetPasswordRequest;
import com.onip.facm01.agent.dto.SetActiveRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agents")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public List<AgentDto> list() {
        return agentService.listAgents().stream().map(AgentDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentDto create(@Valid @RequestBody CreateAgentRequest request) {
        Agent agent = agentService.createAgent(request.username(), request.password(), request.fullName(), request.role());
        return AgentDto.from(agent);
    }

    @PatchMapping("/{id}/active")
    public AgentDto setActive(@PathVariable UUID id, @Valid @RequestBody SetActiveRequest request) {
        return AgentDto.from(agentService.setActive(id, request.active()));
    }

    @PatchMapping("/{id}/role")
    public AgentDto changeRole(@PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest request) {
        return AgentDto.from(agentService.changeRole(id, request.role()));
    }

    @PostMapping("/{id}/reset-password")
    public AgentDto resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        return AgentDto.from(agentService.resetPassword(id, request.newPassword()));
    }
}
