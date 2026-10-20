package com.onip.facm01.site;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.household.dto.SyncResponse;
import com.onip.facm01.security.JwtAuthFilter;
import com.onip.facm01.site.dto.SiteDto;
import com.onip.facm01.site.dto.SiteSyncRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;
    private final AgentRepository agentRepository;

    public SiteController(SiteService siteService, AgentRepository agentRepository) {
        this.siteService = siteService;
        this.agentRepository = agentRepository;
    }

    @PostMapping("/sync")
    public SyncResponse sync(@Valid @RequestBody SiteSyncRequest request, HttpServletRequest httpRequest) {
        Agent agent = currentAgent(httpRequest);
        return siteService.sync(request.sites(), agent);
    }

    @GetMapping
    public Page<SiteDto> list(@PageableDefault(size = 50) Pageable pageable, HttpServletRequest httpRequest) {
        return siteService.list(pageable, currentAgent(httpRequest));
    }

    @GetMapping("/{id}")
    public SiteDto get(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return siteService.get(id, currentAgent(httpRequest));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, HttpServletRequest httpRequest) {
        siteService.delete(id, currentAgent(httpRequest));
    }

    private Agent currentAgent(HttpServletRequest request) {
        Object agentId = request.getAttribute(JwtAuthFilter.AGENT_ID_ATTRIBUTE);
        if (agentId == null) {
            return null;
        }
        return agentRepository.findById(UUID.fromString(agentId.toString())).orElse(null);
    }
}
