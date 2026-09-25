package com.onip.facm01.dashboard;

import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.household.HouseholdStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DIRECTION_GENERALE')")
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
                householdRepository.countByArchivedFalse(),
                householdRepository.countByStatusAndArchivedFalse(HouseholdStatus.COMPLET),
                householdRepository.countByArchivedFalse() - householdRepository.countByStatusAndArchivedFalse(HouseholdStatus.COMPLET),
                dashboardService.populationTotal(),
                dashboardService.registrationCounts(),
                dashboardService.monthlyRegistrations(12));
    }

    public record DashboardStatsDto(
            long total,
            long countComplet,
            long countIncomplet,
            long population,
            DashboardService.RegistrationCounts registrationCounts,
            List<DashboardService.MonthlyCount> monthlyRegistrations) {
    }
}
