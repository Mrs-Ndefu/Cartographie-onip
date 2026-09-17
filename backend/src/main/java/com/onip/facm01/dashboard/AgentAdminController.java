package com.onip.facm01.dashboard;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRole;
import com.onip.facm01.agent.AgentService;
import com.onip.facm01.agent.dto.AgentDto;
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

    public AgentAdminController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        var agentDtos = agentService.listAgents().stream().map(AgentDto::from).toList();
        model.addAttribute("agents", agentDtos);
        model.addAttribute("roles", AgentRole.values());
        agentDtos.stream()
                .filter(a -> a.username().equals(authentication.getName()))
                .findFirst()
                .ifPresent(a -> model.addAttribute("currentAgent", a));
        return "agents";
    }

    @PostMapping
    public String create(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam AgentRole role,
            RedirectAttributes redirectAttributes) {
        try {
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
            if (role != AgentRole.ADMIN && target.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas retirer votre propre rôle ADMIN.");
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
            RedirectAttributes redirectAttributes) {
        try {
            if (newPassword == null || newPassword.length() < 6) {
                throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
            }
            Agent target = agentService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("success", "Mot de passe réinitialisé pour " + target.getUsername());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/agents";
    }
}
