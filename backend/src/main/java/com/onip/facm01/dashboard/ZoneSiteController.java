package com.onip.facm01.dashboard;

import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.zone.Zone;
import com.onip.facm01.zone.ZoneService;
import com.onip.facm01.zone.ZoneSite;
import com.onip.facm01.zone.ZoneSiteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.UUID;

// Page « Sites » (Zones > Ajout site) : sites importés depuis Excel et leur zone d'affectation.
// Consultation : ADMIN, SUPER_ADMIN, SUPERVISEUR ; import, affectation et suppression : ADMIN
// (règle POST /dashboard/zones/** de SecurityConfig).
@Controller
@RequestMapping("/dashboard/zones/sites")
public class ZoneSiteController {

    private static final int PAGE_SIZE = 50;

    private final ZoneSiteService zoneSiteService;
    private final ZoneService zoneService;
    private final AgentRepository agentRepository;

    public ZoneSiteController(ZoneSiteService zoneSiteService, ZoneService zoneService, AgentRepository agentRepository) {
        this.zoneSiteService = zoneSiteService;
        this.zoneService = zoneService;
        this.agentRepository = agentRepository;
    }

    // zone : vide = tous les sites, "none" = non affectés, sinon l'id d'une zone.
    @GetMapping
    public String list(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        String zoneFilter = "all";
        UUID zoneId = null;
        if ("none".equals(zone)) {
            zoneFilter = "none";
        } else if (zone != null && !zone.isBlank()) {
            try {
                zoneId = UUID.fromString(zone);
                zoneFilter = "zone";
            } catch (IllegalArgumentException ignored) {
                zone = null;
            }
        }
        Page<ZoneSite> sites = zoneSiteService.search(zoneFilter, zoneId, search, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        List<Zone> zones = zoneService.list();
        model.addAttribute("sites", sites.getContent());
        model.addAttribute("visitedIds", zoneSiteService.visitedIds(sites.getContent()));
        model.addAttribute("page", sites);
        model.addAttribute("zones", zones);
        model.addAttribute("siteCounts", zoneSiteService.countsByZone());
        model.addAttribute("unassignedCount", zoneSiteService.countUnassigned());
        model.addAttribute("totalCount", zoneSiteService.countAll());
        model.addAttribute("zone", zone == null ? "" : zone);
        // Zone choisie (arrivée depuis le nombre de sites de la page Zones) : la page n'affiche
        // alors que ses sites, sans le bloc d'import.
        UUID selectedId = zoneId;
        model.addAttribute("selectedZone", selectedId == null ? null
                : zones.stream().filter(z -> z.getId().equals(selectedId)).findFirst().orElse(null));
        model.addAttribute("search", search == null ? "" : search);
        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));
        return "zone-sites";
    }

    @GetMapping("/modele.xlsx")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"modele-sites.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(zoneSiteService.template());
    }

    @PostMapping("/import")
    public String importSites(@RequestParam MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            ZoneSiteService.ImportResult r = zoneSiteService.importFrom(file);
            StringBuilder message = new StringBuilder()
                    .append(r.imported()).append(" site(s) importé(s) : ")
                    .append(r.assigned()).append(" affecté(s) à une zone, ")
                    .append(r.unassigned()).append(" non affecté(s).");
            if (r.duplicates() > 0) {
                message.append(' ').append(r.duplicates()).append(" ligne(s) déjà présente(s) ignorée(s).");
            }
            redirectAttributes.addFlashAttribute("success", message.toString());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/zones/sites";
    }

    @PostMapping("/auto-assign")
    public String autoAssign(RedirectAttributes redirectAttributes) {
        int count = zoneSiteService.autoAssignUnassigned();
        redirectAttributes.addFlashAttribute("success", count == 0
                ? "Aucun site non affecté ne correspond à une zone existante."
                : count + " site(s) affecté(s) automatiquement à une zone.");
        return "redirect:/dashboard/zones/sites";
    }

    // Affectation manuelle ; zoneId vide = retirer le site de sa zone. Revient sur la même vue
    // (filtre, recherche, page) que celle d'où l'action a été faite.
    @PostMapping("/{id}/zone")
    public RedirectView assign(
            @PathVariable UUID id,
            @RequestParam(required = false) String zoneId,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        try {
            ZoneSite site = zoneSiteService.assign(id, zoneId == null || zoneId.isBlank() ? null : UUID.fromString(zoneId));
            redirectAttributes.addFlashAttribute("success", site.getZone() == null
                    ? "« " + site.getNom() + " » n'est plus affecté à une zone."
                    : "« " + site.getNom() + " » affecté à la " + site.getZone().getName() + ".");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return new RedirectView(safeReturn(returnTo), true);
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable UUID id,
            @RequestParam(required = false) String returnTo,
            Authentication authentication,
            Model model) {
        model.addAttribute("site", zoneSiteService.find(id));
        model.addAttribute("provinces", com.onip.facm01.household.DrcProvinces.PROVINCES);
        model.addAttribute("returnTo", safeReturn(returnTo));
        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));
        return "zone-site-edit";
    }

    @PostMapping("/{id}/edit")
    public RedirectView edit(
            @PathVariable UUID id,
            @RequestParam String nom,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String territoire,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        try {
            ZoneSite site = zoneSiteService.update(id, nom, adresse, province, ville, territoire);
            redirectAttributes.addFlashAttribute("success", "Site modifié : " + site.getNom()
                    + (site.getZone() == null ? " (non affecté à une zone)." : " — " + site.getZone().getName() + "."));
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return new RedirectView("/dashboard/zones/sites/" + id + "/edit?returnTo="
                    + java.net.URLEncoder.encode(safeReturn(returnTo), java.nio.charset.StandardCharsets.UTF_8), true);
        }
        return new RedirectView(safeReturn(returnTo), true);
    }

    @PostMapping("/{id}/delete")
    public RedirectView delete(
            @PathVariable UUID id,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        try {
            String nom = zoneSiteService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Site supprimé : " + nom);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return new RedirectView(safeReturn(returnTo), true);
    }

    @PostMapping("/delete-all")
    public String deleteAll(RedirectAttributes redirectAttributes) {
        int count = zoneSiteService.deleteAll();
        redirectAttributes.addFlashAttribute("success", count == 0
                ? "Aucun site à supprimer."
                : count + " site(s) supprimé(s).");
        return "redirect:/dashboard/zones/sites";
    }

    // N'accepte qu'un retour vers la page Sites elle-même (pas de redirection ouverte).
    private static String safeReturn(String returnTo) {
        if (returnTo != null && returnTo.startsWith("/dashboard/zones/sites") && !returnTo.contains("//")) {
            return returnTo;
        }
        return "/dashboard/zones/sites";
    }
}
