package com.onip.facm01.dashboard;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.dto.AgentDto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

// Page "Historique" réservée au SUPERVISEUR (cf. SecurityConfig).
@Controller
public class SupervisorHistoryController {

    private static final int PAGE_SIZE = 20;

    private final SupervisorHistoryService historyService;
    private final AgentRepository agentRepository;

    public SupervisorHistoryController(SupervisorHistoryService historyService, AgentRepository agentRepository) {
        this.historyService = historyService;
        this.agentRepository = agentRepository;
    }

    @GetMapping("/dashboard/historique")
    public String history(
            @RequestParam(required = false) UUID agentId,
            @RequestParam(defaultValue = "false") boolean horsZone,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        Agent superviseur = agentRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));
        SupervisorHistoryService.History history = historyService.history(superviseur, agentId, horsZone);

        // Pagination simple sur la liste déjà filtrée (volume limité aux agents du superviseur).
        int totalPages = Math.max(1, (int) Math.ceil(history.rows().size() / (double) PAGE_SIZE));
        int current = Math.min(Math.max(page, 0), totalPages - 1);
        List<SupervisorHistoryService.HistoryRow> rows = history.rows().stream()
                .skip((long) current * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .toList();

        model.addAttribute("currentAgent", AgentDto.from(superviseur));
        model.addAttribute("agents", history.agents().stream().map(AgentDto::from).toList());
        model.addAttribute("rows", rows);
        model.addAttribute("totalRows", history.rows().size());
        model.addAttribute("inZone", history.inZone());
        model.addAttribute("outOfZone", history.outOfZone());
        model.addAttribute("noZone", history.noZone());
        model.addAttribute("agentId", agentId);
        model.addAttribute("horsZone", horsZone);
        model.addAttribute("currentPage", current);
        model.addAttribute("totalPages", totalPages);
        return "historique";
    }
}
