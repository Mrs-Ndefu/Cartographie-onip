package com.onip.facm01.site;

import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.site.dto.SiteDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

// Consultation des fiches de repérage de site depuis le tableau de bord — réservé à
// SUPER_ADMIN/ADMIN/SUPERVISEUR (même règle que la liste des ménages, cf. SecurityConfig : la
// règle générale GET /dashboard/** couvre déjà ces routes, pas besoin d'y ajouter une entrée
// spécifique). Lecture seule pour l'instant : pas de modification/suppression depuis le web.
@Controller
public class SiteDashboardController {

    private final SiteService siteService;
    private final AgentRepository agentRepository;

    public SiteDashboardController(SiteService siteService, AgentRepository agentRepository) {
        this.siteService = siteService;
        this.agentRepository = agentRepository;
    }

    @GetMapping("/dashboard/sites")
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Authentication authentication,
            Model model) {
        Page<SiteDto> sites = siteService.listAdmin(
                search, PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        addCurrentAgent(authentication, model);
        model.addAttribute("sites", sites.getContent());
        model.addAttribute("page", sites);
        model.addAttribute("search", search);
        return "sites";
    }

    @GetMapping("/dashboard/sites/{id}")
    public String detail(@PathVariable UUID id, Authentication authentication, Model model) {
        addCurrentAgent(authentication, model);
        model.addAttribute("site", siteService.get(id));
        return "site-detail";
    }

    private void addCurrentAgent(Authentication authentication, Model model) {
        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));
    }
}
