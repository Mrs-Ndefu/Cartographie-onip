package com.onip.facm01.dashboard;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.AgentRole;
import com.onip.facm01.agent.AgentService;
import com.onip.facm01.agent.dto.AgentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/dashboard/agents")
public class AgentAdminController {

    private final AgentService agentService;
    private final AgentRepository agentRepository;

    public AgentAdminController(AgentService agentService, AgentRepository agentRepository) {
        this.agentService = agentService;
        this.agentRepository = agentRepository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        Page<AgentDto> agentPage = agentService
                .listAgents(PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(AgentDto::from);
        model.addAttribute("agents", agentPage.getContent());
        model.addAttribute("page", agentPage);

        Agent actor = actor(authentication);
        AgentRole actorRole = actor.getRole();
        model.addAttribute("actorRole", actorRole);
        model.addAttribute("hasFullDashboard", actorRole.isAdminTier());
        model.addAttribute("roles", actorRole.assignableRoles());
        model.addAttribute("currentAgent", AgentDto.from(actor));
        return "agents";
    }

    @PostMapping
    public String create(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam AgentRole role,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            requireCanManage(authentication, role);
            if (password == null || password.length() < 6) {
                throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
            }
            agentService.createAgent(username, password, fullName, role);
            redirectAttributes.addFlashAttribute("success", "Agent \"" + username + "\" créé.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/agents";
    }

    @PostMapping("/{id}/active")
    public String setActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Agent target = agentService.getAgent(id);
            requireCanManage(authentication, target.getRole());
            if (!active && target.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas désactiver votre propre compte.");
                return "redirect:/dashboard/agents";
            }
            agentService.setActive(id, active);
            redirectAttributes.addFlashAttribute("success",
                    (active ? "Agent réactivé : " : "Agent désactivé : ") + target.getUsername());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/agents";
    }

    @PostMapping("/{id}/role")
    public String changeRole(
            @PathVariable UUID id,
            @RequestParam AgentRole role,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Agent target = agentService.getAgent(id);
            requireCanManage(authentication, target.getRole());
            requireCanManage(authentication, role);
            boolean losesDashboardAccess = !role.isAdminTier() && role != AgentRole.SUPERVISEUR;
            if (losesDashboardAccess && target.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas retirer votre propre rôle admin.");
                return "redirect:/dashboard/agents";
            }
            agentService.changeRole(id, role);
            redirectAttributes.addFlashAttribute("success", "Rôle mis à jour pour " + target.getUsername() + " : " + role);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/agents";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable UUID id,
            @RequestParam String newPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Agent target = agentService.getAgent(id);
            requireCanManage(authentication, target.getRole());
            if (newPassword == null || newPassword.length() < 6) {
                throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
            }
            agentService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("success", "Mot de passe réinitialisé pour " + target.getUsername());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/agents";
    }

    // Seul un SUPER_ADMIN peut créer/modifier un compte admin-tier (ADMIN ou SUPER_ADMIN) ; un
    // ADMIN gère SUPERVISEUR et AGENT ; un SUPERVISEUR ne gère que les AGENT (cf. AgentRole).
    private void requireCanManage(Authentication authentication, AgentRole roleInvolved) {
        if (!roleInvolved.canBeManagedBy(actor(authentication).getRole())) {
            throw new IllegalArgumentException("Vous n'avez pas les droits pour gérer ce rôle.");
        }
    }

    private Agent actor(Authentication authentication) {
        return agentRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));
    }
}
