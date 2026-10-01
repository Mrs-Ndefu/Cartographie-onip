package com.onip.facm01.dashboard;

import com.onip.facm01.household.HouseholdMemberRepository;
import com.onip.facm01.household.Sexe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Statistiques démographiques de la page Direction Générale : répartition par sexe, mineurs /
// majeurs et tranches d'âge, sur les personnes recensées (chefs et membres) des ménages non
// retirés. L'âge est calculé à partir de la date de naissance saisie ; majorité à 18 ans. Les
// personnes dont le sexe ou la date de naissance (valide) n'est pas renseigné ne sont pas comptées.
@Service
public class DemographicsService {

    public static final int MAJORITE = 18;
    private static final int AGE_MAX_PLAUSIBLE = 120;

    // Les apps saisissent JJ/MM/AAAA ; on accepte aussi quelques variantes (jour/mois sur un
    // chiffre, format ISO) plutôt que d'écarter ces personnes des statistiques.
    private static final List<DateTimeFormatter> FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE);

    // Tranches d'âge du graphique : les trois premières sont des mineurs.
    private static final int[][] TRANCHES = { {0, 4}, {5, 11}, {12, 17}, {18, 24}, {25, 34}, {35, 59}, {60, AGE_MAX_PLAUSIBLE} };

    public record AgeGroup(String label, long hommes, long femmes) {
        public long total() {
            return hommes + femmes;
        }
    }

    public record Demographics(long total, long hommes, long femmes, long mineurs, long majeurs, List<AgeGroup> groups) {
    }

    private record Person(Sexe sexe, Integer age) {
    }

    private final HouseholdMemberRepository householdMemberRepository;

    public DemographicsService(HouseholdMemberRepository householdMemberRepository) {
        this.householdMemberRepository = householdMemberRepository;
    }

    // sexe : "M", "F" ou vide (tous) ; age : "mineurs", "majeurs" ou vide (tous). Tous les
    // chiffres renvoyés portent sur les personnes qui passent ces deux filtres.
    @Transactional(readOnly = true)
    public Demographics demographics(String sexe, String age) {
        LocalDate today = LocalDate.now(DashboardService.ZONE);
        List<Person> people = householdMemberRepository.findSexeAndDateNaissanceOfActiveHouseholds().stream()
                .map(row -> new Person((Sexe) row[0], age((String) row[1], today)))
                .filter(p -> p.sexe() != null && p.age() != null)
                .filter(p -> sexe == null || sexe.isBlank() || p.sexe().name().equals(sexe))
                .filter(p -> age == null || age.isBlank()
                        || ("mineurs".equals(age) && p.age() < MAJORITE)
                        || ("majeurs".equals(age) && p.age() >= MAJORITE))
                .toList();

        long hommes = people.stream().filter(p -> p.sexe() == Sexe.M).count();
        long femmes = people.stream().filter(p -> p.sexe() == Sexe.F).count();
        long mineurs = people.stream().filter(p -> p.age() < MAJORITE).count();
        long majeurs = people.size() - mineurs;

        List<AgeGroup> groups = new ArrayList<>();
        for (int[] tranche : TRANCHES) {
            List<Person> inGroup = people.stream()
                    .filter(p -> p.age() >= tranche[0] && p.age() <= tranche[1])
                    .toList();
            String label = tranche[1] >= AGE_MAX_PLAUSIBLE ? tranche[0] + " ans et +" : tranche[0] + "–" + tranche[1] + " ans";
            groups.add(new AgeGroup(label,
                    inGroup.stream().filter(p -> p.sexe() == Sexe.M).count(),
                    inGroup.stream().filter(p -> p.sexe() == Sexe.F).count()));
        }

        return new Demographics(people.size(), hommes, femmes, mineurs, majeurs, groups);
    }

    // Âge révolu à la date du jour, ou null si la date est absente, illisible ou invraisemblable
    // (dans le futur, plus de 120 ans).
    static Integer age(String dateNaissance, LocalDate today) {
        if (dateNaissance == null || dateNaissance.isBlank()) {
            return null;
        }
        for (DateTimeFormatter format : FORMATS) {
            try {
                LocalDate birth = LocalDate.parse(dateNaissance.trim(), format);
                if (birth.isAfter(today)) {
                    return null;
                }
                int years = Period.between(birth, today).getYears();
                return years > AGE_MAX_PLAUSIBLE ? null : years;
            } catch (DateTimeParseException ignored) {
                // format suivant
            }
        }
        return null;
    }
}
