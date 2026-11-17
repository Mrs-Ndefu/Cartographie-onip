package com.onip.facm01.dashboard;

import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.zone.Zone;
import com.onip.facm01.zone.ZoneService;
import com.onip.facm01.zone.ZoneSiteService;
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
    private final ZoneSiteService zoneSiteService;

    public ZoneAdminController(ZoneService zoneService, AgentRepository agentRepository, ZoneSiteService zoneSiteService) {
        this.zoneService = zoneService;
        this.agentRepository = agentRepository;
        this.zoneSiteService = zoneSiteService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        List<Zone> zones = zoneService.list();
        Map<UUID, Long> agentCounts = zones.stream()
                .collect(Collectors.toMap(Zone::getId, zone -> zoneService.agentCount(zone.getId())));
        model.addAttribute("zones", zones);
        model.addAttribute("agentCounts", agentCounts);
        model.addAttribute("siteCounts", zoneSiteService.countsByZone());
        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));
        return "zones";
    }

    @PostMapping
    public String create(
            @RequestParam String province,
            @RequestParam String ville,
            @RequestParam(required = false) List<String> communes,
            @RequestParam(required = false) List<String> quartiers,
            RedirectAttributes redirectAttributes) {
        try {
            Zone zone = zoneService.create(province, ville, communes, quartiers);
            redirectAttributes.addFlashAttribute("success", "Zone créée : " + zone.getLabel());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/zones";
    }

    // Quartiers d'une zone (ADMIN) : compléter une zone créée avant l'ajout des quartiers, ou
    // ajuster la liste.
    @GetMapping("/{id}/quartiers")
    public String quartiersForm(@PathVariable UUID id, Authentication authentication, Model model)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        Zone zone = zoneService.get(id);
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        model.addAttribute("zone", zone);
        // Communes et quartiers déjà choisis, lus par app.js (data-communes / data-selected).
        model.addAttribute("communesJson", json.writeValueAsString(zone.getCommunes()));
        model.addAttribute("quartiersJson", json.writeValueAsString(zone.getQuartiers()));
        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));
        return "zone-quartiers";
    }

    @PostMapping("/{id}/quartiers")
    public String saveQuartiers(
            @PathVariable UUID id,
            @RequestParam(required = false) List<String> quartiers,
            RedirectAttributes redirectAttributes) {
        try {
            Zone zone = zoneService.updateQuartiers(id, quartiers);
            redirectAttributes.addFlashAttribute("success",
                    zone.getName() + " : " + zone.getQuartiers().size() + " quartier(s) enregistré(s).");
            return "redirect:/dashboard/zones";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/zones/" + id + "/quartiers";
        }
    }

    @PostMapping("/delete-all")
    public String deleteAll(RedirectAttributes redirectAttributes) {
        int deleted = zoneService.deleteAll();
        redirectAttributes.addFlashAttribute("success", deleted == 0
                ? "Aucune zone à supprimer."
                : deleted + " zone(s) supprimée(s) ; les comptes affectés n'ont plus de zone.");
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
