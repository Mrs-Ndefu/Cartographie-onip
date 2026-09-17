package com.onip.facm01.household;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.household.dto.HouseholdDto;
import com.onip.facm01.household.dto.HouseholdSyncRequest;
import com.onip.facm01.household.dto.SyncResponse;
import com.onip.facm01.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/households")
public class HouseholdController {

    private final HouseholdService householdService;
    private final AgentRepository agentRepository;

    public HouseholdController(HouseholdService householdService, AgentRepository agentRepository) {
        this.householdService = householdService;
        this.agentRepository = agentRepository;
    }

    @PostMapping("/sync")
    public SyncResponse sync(@Valid @RequestBody HouseholdSyncRequest request, HttpServletRequest httpRequest) {
        Agent agent = currentAgent(httpRequest);
        return householdService.sync(request.households(), agent);
    }

    @GetMapping
    public Page<HouseholdDto> list(@PageableDefault(size = 50) Pageable pageable) {
        return householdService.list(pageable);
    }

    @GetMapping("/{id}")
    public HouseholdDto get(@PathVariable UUID id) {
        return householdService.get(id);
    }

    private Agent currentAgent(HttpServletRequest request) {
        Object agentId = request.getAttribute(JwtAuthFilter.AGENT_ID_ATTRIBUTE);
        if (agentId == null) {
            return null;
        }
        return agentRepository.findById(UUID.fromString(agentId.toString())).orElse(null);
    }
}
