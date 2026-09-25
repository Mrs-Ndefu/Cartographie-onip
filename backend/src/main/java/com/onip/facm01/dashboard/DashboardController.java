package com.onip.facm01.dashboard;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.household.DrcProvinces;
import com.onip.facm01.household.HouseholdFilter;
import com.onip.facm01.household.HouseholdPhoto;
import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.household.HouseholdService;
import com.onip.facm01.household.HouseholdStatus;
import com.onip.facm01.household.Sexe;
import com.onip.facm01.household.dto.HouseholdDto;
import com.onip.facm01.household.dto.HouseholdEditForm;
import com.onip.facm01.zone.Zone;
import com.onip.facm01.zone.ZoneService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Controller
public class DashboardController {

    // Chef + 14 membres, comme la fiche papier (même limite que MAX_MEMBRES côté web).
    private static final int MAX_MEMBRES = 15;

    private final HouseholdRepository householdRepository;
    private final HouseholdService householdService;
    private final DashboardService dashboardService;
    private final AgentRepository agentRepository;
    private final ZoneService zoneService;

    public DashboardController(
            HouseholdRepository householdRepository,
            HouseholdService householdService,
            DashboardService dashboardService,
            AgentRepository agentRepository,
            ZoneService zoneService) {
        this.householdRepository = householdRepository;
        this.householdService = householdService;
        this.dashboardService = dashboardService;
        this.agentRepository = agentRepository;
        this.zoneService = zoneService;
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
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String commune,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication,
            Model model) {
        HouseholdFilter filter = filter(search, province, ville, commune, statut, zoneId, dateFrom, dateTo);
        Page<HouseholdDto> households = householdService.searchAdmin(
                filter, false, PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "updatedAt")));

        addCurrentAgent(authentication, model);

        model.addAttribute("households", households.getContent());
        model.addAttribute("page", households);
        model.addAttribute("search", search);
        model.addAttribute("province", province);
        model.addAttribute("ville", ville);
        model.addAttribute("commune", commune);
        model.addAttribute("statut", statut);
        model.addAttribute("zoneId", zoneId);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("provinces", householdService.distinctProvinces());
        model.addAttribute("villes", householdService.distinctVilles(province, commune));
        model.addAttribute("communes", householdService.distinctCommunes(province, ville));
        model.addAttribute("zones", zoneService.list());

        // Carte : tous les ménages filtrés (pas seulement la page), et zoom sur eux dès qu'un
        // filtre géographique (province, ville, commune ou zone) est choisi.
        model.addAttribute("mapPoints", householdService.mapPoints(filter));
        model.addAttribute("zoomToPoints", filter.hasPlaceFilter());

        long totalActive = householdRepository.countByArchivedFalse();
        long countComplet = householdRepository.countByStatusAndArchivedFalse(HouseholdStatus.COMPLET);
        // Volontairement tous ménages confondus (retirés ou non) : ce compteur ne doit pas
        // bouger quand un ménage est retiré/restauré, seul le compteur "Retirés" en dessous
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

    // Page de la DIRECTION_GENERALE (accessible aussi aux autres rôles du tableau de bord) :
    // uniquement les statistiques d'enregistrement et la population totale.
    @GetMapping("/dashboard/stats")
    public String stats(Authentication authentication, Model model) {
        addCurrentAgent(authentication, model);
        model.addAttribute("total", householdRepository.count());
        model.addAttribute("population", dashboardService.populationTotal());
        model.addAttribute("registrationCounts", dashboardService.registrationCounts());
        model.addAttribute("monthlyRegistrations", dashboardService.monthlyRegistrations(12));
        return "stats";
    }

    @GetMapping("/dashboard/households/{id}")
    public String householdDetail(@PathVariable UUID id, Authentication authentication, Model model) {
        addCurrentAgent(authentication, model);
        model.addAttribute("household", householdService.get(id));
        model.addAttribute("modifications", householdService.modifications(id));
        return "household-detail";
    }

    // Page complète de modification (utilisée depuis le détail d'un ménage, et en repli quand un
    // enregistrement depuis le tableau est refusé : le formulaire saisi y est réaffiché).
    @GetMapping("/dashboard/households/{id}/edit")
    public String editHousehold(
            @PathVariable UUID id,
            @RequestParam(required = false) String returnTo,
            Authentication authentication,
            Model model) {
        addCurrentAgent(authentication, model);
        addEditModel(model, id, returnTo);
        return "household-edit";
    }

    // Le même formulaire, seul, chargé dans une fenêtre par-dessus le tableau des ménages : la
    // modification se fait sans quitter la liste.
    @GetMapping("/dashboard/households/{id}/edit-form")
    public String editHouseholdFragment(
            @PathVariable UUID id,
            @RequestParam(required = false) String returnTo,
            Model model) {
        addEditModel(model, id, returnTo);
        return "fragments/household-edit-form :: form";
    }

    @PostMapping("/dashboard/households/{id}/edit")
    public String saveHousehold(
            @PathVariable UUID id,
            @ModelAttribute("form") HouseholdEditForm form,
            @RequestParam(required = false) String returnTo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        // Lignes membres retirées dans le formulaire : indices non contigus, donc entrées nulles.
        form.getMembres().removeIf(Objects::isNull);
        try {
            Agent author = agentRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));
            householdService.updateFromDashboard(id, form, author);
            redirectAttributes.addFlashAttribute("success", "Ménage mis à jour.");
            return "redirect:" + (isDashboardPath(returnTo) ? returnTo : "/dashboard/households/" + id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            redirectAttributes.addAttribute("returnTo", isDashboardPath(returnTo) ? returnTo : null);
            return "redirect:/dashboard/households/" + id + "/edit";
        }
    }

    private void addEditModel(Model model, UUID id, String returnTo) {
        HouseholdDto household = householdService.get(id);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", HouseholdEditForm.from(household));
        }
        model.addAttribute("returnTo", isDashboardPath(returnTo) ? returnTo : null);
        model.addAttribute("maxMembres", MAX_MEMBRES);
        addEditOptions(model, household);
    }

    // Redirection après enregistrement limitée aux pages du tableau de bord (pas de redirection
    // ouverte vers un autre site).
    private static boolean isDashboardPath(String path) {
        return path != null && path.startsWith("/dashboard") && !path.startsWith("//");
    }

    @GetMapping("/dashboard/households/{id}/photo/{position}")
    @ResponseBody
    public ResponseEntity<byte[]> householdPhoto(@PathVariable UUID id, @PathVariable int position) {
        HouseholdPhoto photo = householdService.getPhoto(id, position);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getPhotoContentType()))
                .body(photo.getPhoto());
    }

    // Ménages retirés (anciennement "archivés") — l'URL garde son nom technique.
    @GetMapping("/dashboard/archives")
    public String archives(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Authentication authentication,
            Model model) {
        HouseholdFilter filter = new HouseholdFilter(search, null, null, null, null, null, null, null);
        Page<HouseholdDto> households = householdService.searchAdmin(
                filter, true, PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "archivedAt")));

        addCurrentAgent(authentication, model);

        model.addAttribute("households", households.getContent());
        model.addAttribute("page", households);
        model.addAttribute("search", search);
        return "archives";
    }

    // Recherche "en direct" (auto-filtrage dès la première lettre) consommée en AJAX par le
    // champ de recherche — renvoie du JSON, pas de vue, d'où @ResponseBody sur ce contrôleur
    // par ailleurs orienté Thymeleaf. Plafonné à 50 résultats : pensé pour affiner une saisie,
    // pas pour remplacer la liste paginée complète. Reprend les mêmes filtres que /dashboard afin
    // que le total affiché reste cohérent avec la recherche en cours.
    @GetMapping("/dashboard/households/search")
    @ResponseBody
    public List<HouseholdDto> searchHouseholdsJson(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String commune,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return householdService.searchAdmin(
                filter(q, province, ville, commune, statut, zoneId, dateFrom, dateTo), false,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "updatedAt"))).getContent();
    }

    @PostMapping("/dashboard/households/{id}/archive")
    public String archiveHousehold(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            householdService.archive(id);
            redirectAttributes.addFlashAttribute("success", "Ménage retiré.");
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

    // Une zone choisie remplace les filtres province/ville/commune par les siens, et ajoute son
    // quartier s'il en a un.
    private HouseholdFilter filter(
            String search, String province, String ville, String commune, String statut, UUID zoneId,
            LocalDate dateFrom, LocalDate dateTo) {
        String quartier = null;
        // Une zone supprimée entre-temps (lien ou page restés ouverts) est simplement ignorée.
        Zone zone = zoneId == null ? null : zoneService.find(zoneId).orElse(null);
        if (zone != null) {
            province = zone.getProvince();
            ville = zone.getVille();
            commune = zone.getCommune();
            quartier = zone.getQuartier();
        }
        return new HouseholdFilter(
                search, province, ville, commune, quartier, statut, toStartOfDay(dateFrom), toEndOfDay(dateTo));
    }

    private void addEditOptions(Model model, HouseholdDto household) {
        model.addAttribute("household", household);
        model.addAttribute("provinces", DrcProvinces.PROVINCES);
        model.addAttribute("statuses", HouseholdStatus.values());
        model.addAttribute("sexes", Sexe.values());
    }

    private void addCurrentAgent(Authentication authentication, Model model) {
        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));
    }

    private static Instant toStartOfDay(LocalDate date) {
        return date == null ? null : DashboardService.startOfDay(date);
    }

    private static Instant toEndOfDay(LocalDate date) {
        return date == null ? null : DashboardService.endOfDay(date);
    }
}
