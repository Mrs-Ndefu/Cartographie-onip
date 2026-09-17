package com.onip.facm01.dashboard;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.AgentService;
import com.onip.facm01.agent.dto.AgentDto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.UncheckedIOException;

@Controller
@RequestMapping("/dashboard/profile")
public class ProfileController {

    private final AgentService agentService;
    private final AgentRepository agentRepository;

    public ProfileController(AgentService agentService, AgentRepository agentRepository) {
        this.agentService = agentService;
        this.agentRepository = agentRepository;
    }

    @GetMapping
    public String view(Authentication authentication, Model model) {
        model.addAttribute("agent", AgentDto.from(currentAgent(authentication)));
        return "profile";
    }

    @PostMapping("/photo")
    public String uploadPhoto(
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Agent agent = currentAgent(authentication);
            agentService.updatePhoto(agent.getId(), file.getBytes(), file.getContentType());
            redirectAttributes.addFlashAttribute("success", "Photo mise à jour.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le fichier envoyé", e);
        }
        return "redirect:/dashboard/profile";
    }

    @PostMapping("/password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("Les deux mots de passe ne correspondent pas");
            }
            Agent agent = currentAgent(authentication);
            agentService.changeOwnPassword(agent.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Mot de passe mis à jour.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/profile";
    }

    private Agent currentAgent(Authentication authentication) {
        return agentRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Agent introuvable"));
    }
}
