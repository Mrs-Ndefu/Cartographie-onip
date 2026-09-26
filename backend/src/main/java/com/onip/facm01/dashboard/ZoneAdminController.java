package com.onip.facm01.dashboard;

import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.household.DrcProvinces;
import com.onip.facm01.zone.Zone;
import com.onip.facm01.zone.ZoneService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Zones d'affectation des agents : définies par l'ADMIN (création/suppression, cf.
// SecurityConfig), consultables par le SUPER_ADMIN et le SUPERVISEUR.
@Controller
@RequestMapping("/dashboard/zones")
public class ZoneAdminController {

    private final ZoneService zoneService;
    private final AgentRepository agentRepository;

    public ZoneAdminController(ZoneService zoneService, AgentRepository agentRepository) {
        this.zoneService = zoneService;
        this.agentRepository = agentRepository;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        List<Zone> zones = zoneService.list();
        Map<UUID, Long> agentCounts = zones.stream()
                .collect(Collectors.toMap(Zone::getId, zone -> zoneService.agentCount(zone.getId())));
        model.addAttribute("zones", zones);
        model.addAttribute("agentCounts", agentCounts);
        model.addAttribute("provinces", DrcProvinces.PROVINCES);
        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));
        return "zones";
    }

    @PostMapping
    public String create(
            @RequestParam String province,
            @RequestParam String ville,
            @RequestParam(required = false) List<String> communes,
            RedirectAttributes redirectAttributes) {
        try {
            Zone zone = zoneService.create(province, ville, communes);
            redirectAttributes.addFlashAttribute("success", "Zone créée : " + zone.getLabel());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/zones";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            String label = zoneService.get(id).getLabel();
            zoneService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Zone supprimée : " + label);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/zones";
    }
}
