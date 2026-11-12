package com.onip.facm01.site;

import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.household.HouseholdStatus;
import com.onip.facm01.site.dto.SiteDto;
import com.onip.facm01.site.dto.SiteEditForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

// Fiches de repérage de site depuis le tableau de bord. Consultation : SUPER_ADMIN/ADMIN/
// SUPERVISEUR (règle générale GET /dashboard/** de SecurityConfig). Valider/rejeter et
// retirer/restaurer : ADMIN et SUPERVISEUR ; modifier : ADMIN seul (règles dédiées dans
// SecurityConfig, cf. AgentRole.canManageSites/canEditSites pour l'affichage des boutons).
@Controller
public class SiteDashboardController {

    private final SiteService siteService;
    private final AgentRepository agentRepository;
    private final HouseholdRepository householdRepository;

    public SiteDashboardController(
            SiteService siteService, AgentRepository agentRepository, HouseholdRepository householdRepository) {
        this.siteService = siteService;
        this.agentRepository = agentRepository;
        this.householdRepository = householdRepository;
    }

    // retirees=true : fiches retirées ou rejetées (équivalent de "Ménages retirés").
    // incompletes=true : fiches signalées par la cloche (incomplètes, en attente d'une action).
    @GetMapping("/dashboard/sites")
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean retirees,
            @RequestParam(defaultValue = "false") boolean incompletes,
            Authentication authentication,
            Model model) {
        Sort sort = retirees
                ? Sort.by(Sort.Direction.DESC, "archivedAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        incompletes = incompletes && !retirees;
        Page<SiteDto> sites = siteService.listAdmin(search, retirees, incompletes, PageRequest.of(page, 20, sort));
        addCurrentAgent(authentication, model);
        model.addAttribute("sites", sites.getContent());
        model.addAttribute("page", sites);
        model.addAttribute("search", search);
        model.addAttribute("retirees", retirees);
        model.addAttribute("incompletes", incompletes);
        return "sites";
    }

    @GetMapping("/dashboard/sites/{id}")
    public String detail(@PathVariable UUID id, Authentication authentication, Model model) {
        addCurrentAgent(authentication, model);
        model.addAttribute("site", siteService.get(id));
        return "site-detail";
    }

    @GetMapping("/dashboard/sites/{id}/edit")
    public String edit(@PathVariable UUID id, Authentication authentication, Model model) {
        addCurrentAgent(authentication, model);
        SiteDto site = siteService.get(id);
        model.addAttribute("site", site);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", SiteEditForm.from(site));
        }
        return "site-edit";
    }

    @PostMapping("/dashboard/sites/{id}/edit")
    public String save(
            @PathVariable UUID id,
            @ModelAttribute("form") SiteEditForm form,
            RedirectAttributes redirectAttributes) {
        try {
            siteService.updateFromDashboard(id, form);
            SiteDto updated = siteService.get(id);
            if (updated.visiteComplete()) {
                redirectAttributes.addFlashAttribute("success", "Fiche de repérage mise à jour — statut : Visité complet.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Fiche enregistrée mais toujours « Visité incomplet » : il manque "
                        + String.join(", ", updated.champsManquants()) + ".");
            }
            return "redirect:/dashboard/sites/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/dashboard/sites/" + id + "/edit";
        }
    }

    @PostMapping("/dashboard/sites/{id}/validate")
    public String validate(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        siteService.validate(id);
        redirectAttributes.addFlashAttribute("success", "Fiche de repérage validée.");
        return "redirect:/dashboard/sites/" + id;
    }

    @PostMapping("/dashboard/sites/{id}/reject")
    public String reject(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        siteService.reject(id);
        redirectAttributes.addFlashAttribute("success",
                "Fiche de repérage rejetée : elle est consultable dans les fiches retirées.");
        return "redirect:/dashboard/sites";
    }

    @PostMapping("/dashboard/sites/{id}/archive")
    public String archive(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        siteService.archive(id);
        redirectAttributes.addFlashAttribute("success", "Fiche de repérage retirée.");
        return "redirect:/dashboard/sites";
    }

    @PostMapping("/dashboard/sites/{id}/restore")
    public String restore(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        siteService.restore(id);
        redirectAttributes.addFlashAttribute("success", "Fiche de repérage restaurée.");
        return "redirect:/dashboard/sites?retirees=true";
    }

    // Ajoute aussi les compteurs de la cloche de notification (fragments/notif-bell.html),
    // présente sur toutes les pages du repérage comme sur le tableau de bord.
    private void addCurrentAgent(Authentication authentication, Model model) {
        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));
        model.addAttribute("countIncomplet", householdRepository.countByArchivedFalse()
                - householdRepository.countByStatusAndArchivedFalse(HouseholdStatus.COMPLET));
        model.addAttribute("countSitesIncomplets", siteService.countPendingIncomplete());
    }
}
