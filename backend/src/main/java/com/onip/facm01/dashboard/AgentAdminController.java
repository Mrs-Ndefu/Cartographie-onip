package com.onip.facm01.dashboard;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.AgentRole;
import com.onip.facm01.agent.AgentService;
import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.zone.Zone;
import com.onip.facm01.zone.ZoneService;
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
    private final ZoneService zoneService;

    public AgentAdminController(AgentService agentService, AgentRepository agentRepository, ZoneService zoneService) {
        this.agentService = agentService;
        this.agentRepository = agentRepository;
        this.zoneService = zoneService;
    }

    // Un SUPERVISEUR ne voit que les agents que l'ADMIN lui a affectés ; les autres rôles
    // voient tous les comptes.
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        Agent actor = actor(authentication);
        AgentRole actorRole = actor.getRole();
        PageRequest pageRequest = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AgentDto> agentPage = (actorRole == AgentRole.SUPERVISEUR
                ? agentService.listAgentsOf(actor.getId(), pageRequest)
                : agentService.listAgents(pageRequest))
                .map(AgentDto::from);
        model.addAttribute("agents", agentPage.getContent());
        model.addAttribute("page", agentPage);

        model.addAttribute("actorRole", actorRole);
        model.addAttribute("actorId", actor.getId());
        model.addAttribute("roles", actorRole.assignableRoles());
        model.addAttribute("zones", zoneService.list());
        model.addAttribute("supervisors", agentService.listSupervisors().stream().map(AgentDto::from).toList());
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
            Agent created = agentService.createAgent(username, password, fullName, role);
            Agent actor = actor(authentication);
            if (actor.getRole() == AgentRole.SUPERVISEUR && role == AgentRole.AGENT) {
                agentService.assignSupervisor(created.getId(), actor);
            }
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
            requireCanManage(authentication, target);
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
            requireCanManage(authentication, target);
            requireCanManage(authentication, role);
            // Changer son propre rôle ferait perdre l'accès à la gestion des comptes (ou au
            // tableau de bord), et on ne pourrait plus revenir en arrière soi-même.
            if (role != target.getRole() && target.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas changer votre propre rôle.");
                return "redirect:/dashboard/agents";
            }
            agentService.changeRole(id, role);
            redirectAttributes.addFlashAttribute("success", "Rôle mis à jour pour " + target.getUsername() + " : " + role);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/agents";
    }

    // zoneId vide = retirer l'affectation.
    @PostMapping("/{id}/zone")
    public String assignZone(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID zoneId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Agent target = agentService.getAgent(id);
            requireCanManage(authentication, target);
            Zone zone = zoneId == null ? null : zoneService.get(zoneId);
            agentService.assignZone(id, zone);
            redirectAttributes.addFlashAttribute("success", zone == null
                    ? "Affectation retirée pour " + target.getUsername()
                    : target.getUsername() + " affecté à la zone " + zone.getLabel());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/agents";
    }

    // Affectation d'un agent à un superviseur, par l'ADMIN (ou le SUPER_ADMIN, qui gère les
    // comptes). superviseurId vide = retirer l'affectation.
    @PostMapping("/{id}/superviseur")
    public String assignSupervisor(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID superviseurId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            if (!actor(authentication).getRole().canAssignSupervisors()) {
                throw new IllegalArgumentException("Seul un administrateur peut affecter un agent à un superviseur.");
            }
            Agent target = agentService.getAgent(id);
            Agent superviseur = superviseurId == null ? null : agentService.getAgent(superviseurId);
            agentService.assignSupervisor(id, superviseur);
            redirectAttributes.addFlashAttribute("success", superviseur == null
                    ? "Superviseur retiré pour " + target.getUsername()
                    : target.getUsername() + " affecté au superviseur " + superviseur.getFullName());
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
            requireCanManage(authentication, target);
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
    // ADMIN gère tous les autres ; un SUPERVISEUR ne gère que les AGENT (cf. AgentRole).
    // Même règle que ci-dessous pour un compte existant, et en plus : un SUPERVISEUR ne gère que
    // les agents qui lui sont affectés.
    private void requireCanManage(Authentication authentication, Agent target) {
        requireCanManage(authentication, target.getRole());
        Agent actor = actor(authentication);
        if (actor.getRole() == AgentRole.SUPERVISEUR
                && (target.getSuperviseur() == null || !target.getSuperviseur().getId().equals(actor.getId()))) {
            throw new IllegalArgumentException("Cet agent ne vous est pas affecté.");
        }
    }

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
