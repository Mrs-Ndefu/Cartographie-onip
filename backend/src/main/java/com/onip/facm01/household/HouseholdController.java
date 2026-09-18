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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    public Page<HouseholdDto> list(@PageableDefault(size = 50) Pageable pageable, HttpServletRequest httpRequest) {
        return householdService.list(pageable, currentAgent(httpRequest));
    }

    @GetMapping("/{id}")
    public HouseholdDto get(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return householdService.get(id, currentAgent(httpRequest));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, HttpServletRequest httpRequest) {
        householdService.delete(id, currentAgent(httpRequest));
    }

    @PostMapping("/{id}/photo")
    public HouseholdDto uploadPhoto(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file, HttpServletRequest httpRequest) {
        Agent agent = currentAgent(httpRequest);
        try {
            householdService.attachPhoto(id, file.getBytes(), file.getContentType(), agent);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le fichier envoyé", e);
        }
        return householdService.get(id, agent);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable UUID id, HttpServletRequest httpRequest) {
        HouseholdPhoto photo = householdService.getPhoto(id, currentAgent(httpRequest));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getPhotoContentType()))
                .body(photo.getPhoto());
    }

    private Agent currentAgent(HttpServletRequest request) {
        Object agentId = request.getAttribute(JwtAuthFilter.AGENT_ID_ATTRIBUTE);
        if (agentId == null) {
            return null;
        }
        return agentRepository.findById(UUID.fromString(agentId.toString())).orElse(null);
    }
}
