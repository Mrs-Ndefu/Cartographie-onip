package com.onip.facm01.dashboard;

import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.household.HouseholdService;
import com.onip.facm01.household.HouseholdStatus;
import com.onip.facm01.household.dto.HouseholdDto;
import com.onip.facm01.household.HouseholdPhoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
public class DashboardController {

    private final HouseholdRepository householdRepository;
    private final HouseholdService householdService;
    private final DashboardService dashboardService;
    private final AgentRepository agentRepository;

    public DashboardController(
            HouseholdRepository householdRepository,
            HouseholdService householdService,
            DashboardService dashboardService,
            AgentRepository agentRepository) {
        this.householdRepository = householdRepository;
        this.householdService = householdService;
        this.dashboardService = dashboardService;
        this.agentRepository = agentRepository;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String commune,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication,
            Model model) {
        Page<HouseholdDto> households = householdService.searchAdmin(
                search, ville, commune, toStartOfDay(dateFrom), toEndOfDay(dateTo), false,
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "updatedAt")));

        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));

        model.addAttribute("households", households.getContent());
        model.addAttribute("page", households);
        model.addAttribute("search", search);
        model.addAttribute("ville", ville);
        model.addAttribute("commune", commune);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("villes", householdService.distinctVilles(commune));
        model.addAttribute("communes", householdService.distinctCommunes(ville));

        long totalActive = householdRepository.countByArchivedFalse();
        long countComplet = householdRepository.countByStatusAndArchivedFalse(HouseholdStatus.COMPLET);
        // Volontairement tous ménages confondus (archivés ou non) : ce compteur ne doit pas
        // bouger quand un ménage est archivé/restauré, seul le compteur "Archivés" en dessous
        // reflète ce mouvement.
        model.addAttribute("total", householdRepository.count());
        model.addAttribute("population", dashboardService.populationTotal());
        model.addAttribute("countComplet", countComplet);
        model.addAttribute("countIncomplet", totalActive - countComplet);
        model.addAttribute("countArchived", householdRepository.countByArchivedTrue());

        model.addAttribute("registrationCounts", dashboardService.registrationCounts());
        model.addAttribute("monthlyRegistrations", dashboardService.monthlyRegistrations(12));

        return "dashboard";
    }

    @GetMapping("/dashboard/households/{id}")
    public String householdDetail(@PathVariable UUID id, Authentication authentication, Model model) {
        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));
        model.addAttribute("household", householdService.get(id));
        return "household-detail";
    }

    @GetMapping("/dashboard/households/{id}/photo/{position}")
    @ResponseBody
    public ResponseEntity<byte[]> householdPhoto(@PathVariable UUID id, @PathVariable int position) {
        HouseholdPhoto photo = householdService.getPhoto(id, position);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getPhotoContentType()))
                .body(photo.getPhoto());
    }

    @GetMapping("/dashboard/archives")
    public String archives(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Authentication authentication,
            Model model) {
        Page<HouseholdDto> households = householdService.searchAdmin(
                search, null, null, null, null, true,
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "archivedAt")));

        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));

        model.addAttribute("households", households.getContent());
        model.addAttribute("page", households);
        model.addAttribute("search", search);
        return "archives";
    }

    // Recherche "en direct" (auto-filtrage dès la première lettre) consommée en AJAX par le
    // champ de recherche — renvoie du JSON, pas de vue, d'où @ResponseBody sur ce contrôleur
    // par ailleurs orienté Thymeleaf. Plafonné à 50 résultats : pensé pour affiner une saisie,
    // pas pour remplacer la liste paginée complète. Reprend les mêmes filtres que /dashboard afin
    // que le total affiché reste cohérent avec la recherche en cours (commune, dates).
    @GetMapping("/dashboard/households/search")
    @ResponseBody
    public List<HouseholdDto> searchHouseholdsJson(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String commune,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return householdService.searchAdmin(
                q, ville, commune, toStartOfDay(dateFrom), toEndOfDay(dateTo), false,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "updatedAt"))).getContent();
    }

    @PostMapping("/dashboard/households/{id}/archive")
    public String archiveHousehold(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            householdService.archive(id);
            redirectAttributes.addFlashAttribute("success", "Ménage archivé.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/households/{id}/restore")
    public String restoreHousehold(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            householdService.restore(id);
            redirectAttributes.addFlashAttribute("success", "Ménage restauré.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/archives";
    }

    private static Instant toStartOfDay(LocalDate date) {
        return date == null ? null : DashboardService.startOfDay(date);
    }

    private static Instant toEndOfDay(LocalDate date) {
        return date == null ? null : DashboardService.endOfDay(date);
    }
}
