package com.onip.facm01.dashboard;

import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.household.HouseholdService;
import com.onip.facm01.household.HouseholdStatus;
import com.onip.facm01.household.dto.HouseholdDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String dashboard(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        Page<HouseholdDto> households = householdService.list(
                PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "updatedAt")));

        agentRepository.findByUsername(authentication.getName())
                .ifPresent(agent -> model.addAttribute("currentAgent", AgentDto.from(agent)));

        model.addAttribute("households", households.getContent());
        model.addAttribute("page", households);
        model.addAttribute("total", householdRepository.count());
        model.addAttribute("countComplet", householdRepository.countByStatus(HouseholdStatus.COMPLET));
        model.addAttribute("countBrouillon", householdRepository.countByStatus(HouseholdStatus.BROUILLON));
        model.addAttribute("countAVerifier", householdRepository.countByStatus(HouseholdStatus.A_VERIFIER));

        model.addAttribute("registrationCounts", dashboardService.registrationCounts());
        model.addAttribute("sexDistribution", dashboardService.sexDistribution());
        model.addAttribute("monthlyRegistrations", dashboardService.monthlyRegistrations(12));

        return "dashboard";
    }
}
