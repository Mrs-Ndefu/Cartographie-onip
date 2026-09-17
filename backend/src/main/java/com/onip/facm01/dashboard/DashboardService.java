package com.onip.facm01.dashboard;

import com.onip.facm01.household.HouseholdMemberRepository;
import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.household.Sexe;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Kinshasa");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;

    public DashboardService(HouseholdRepository householdRepository, HouseholdMemberRepository householdMemberRepository) {
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
    }

    public RegistrationCounts registrationCounts() {
        List<Instant> createdAts = householdRepository.findAllCreatedAt();
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        Instant startOfDay = now.toLocalDate().atStartOfDay(ZONE).toInstant();
        Instant startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay(ZONE).toInstant();
        Instant startOfYear = now.toLocalDate().withDayOfYear(1).atStartOfDay(ZONE).toInstant();

        long today = createdAts.stream().filter(i -> !i.isBefore(startOfDay)).count();
        long thisMonth = createdAts.stream().filter(i -> !i.isBefore(startOfMonth)).count();
        long thisYear = createdAts.stream().filter(i -> !i.isBefore(startOfYear)).count();

        return new RegistrationCounts(today, thisMonth, thisYear);
    }

    public Map<String, Long> sexDistribution() {
        List<Sexe> values = householdMemberRepository.findAllSexe();
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("M", values.stream().filter(s -> s == Sexe.M).count());
        counts.put("F", values.stream().filter(s -> s == Sexe.F).count());
        return counts;
    }

    public List<MonthlyCount> monthlyRegistrations(int monthsBack) {
        List<Instant> createdAts = householdRepository.findAllCreatedAt();
        YearMonth current = YearMonth.now(ZONE);

        List<YearMonth> months = new ArrayList<>();
        for (int i = monthsBack - 1; i >= 0; i--) {
            months.add(current.minusMonths(i));
        }

        Map<YearMonth, Long> counts = createdAts.stream()
                .collect(Collectors.groupingBy(instant -> YearMonth.from(instant.atZone(ZONE)), Collectors.counting()));

        return months.stream()
                .map(ym -> new MonthlyCount(ym.format(MONTH_LABEL), counts.getOrDefault(ym, 0L)))
                .toList();
    }

    public record RegistrationCounts(long today, long thisMonth, long thisYear) {
    }

    public record MonthlyCount(String label, long count) {
    }
}
