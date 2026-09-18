package com.onip.facm01.dashboard;

import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.household.HouseholdStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardApiController {

    private final HouseholdRepository householdRepository;
    private final DashboardService dashboardService;

    public DashboardApiController(HouseholdRepository householdRepository, DashboardService dashboardService) {
        this.householdRepository = householdRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public DashboardStatsDto stats() {
        return new DashboardStatsDto(
                householdRepository.count(),
                householdRepository.countByStatus(HouseholdStatus.COMPLET),
                householdRepository.countByStatus(HouseholdStatus.BROUILLON),
                householdRepository.countByStatus(HouseholdStatus.A_VERIFIER),
                dashboardService.registrationCounts(),
                dashboardService.sexDistribution(),
                dashboardService.monthlyRegistrations(12));
    }

    public record DashboardStatsDto(
            long total,
            long countComplet,
            long countBrouillon,
            long countAVerifier,
            DashboardService.RegistrationCounts registrationCounts,
            Map<String, Long> sexDistribution,
            List<DashboardService.MonthlyCount> monthlyRegistrations) {
    }
}
