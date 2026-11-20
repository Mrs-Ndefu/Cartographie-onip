package com.onip.facm01.zone;

import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.household.DrcProvinces;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final AgentRepository agentRepository;
    private final ZoneSiteService zoneSiteService;

    public ZoneService(ZoneRepository zoneRepository, AgentRepository agentRepository, ZoneSiteService zoneSiteService) {
        this.zoneRepository = zoneRepository;
        this.agentRepository = agentRepository;
        this.zoneSiteService = zoneSiteService;
    }

    // Triées par code : 01-0001, 01-0002, ..., 02-0001... (zones sans code en dernier).
    public List<Zone> list() {
        return zoneRepository.findAllByOrderByProvinceAscVilleAsc().stream()
                .sorted(Comparator.comparingInt(ZoneService::sortKey))
                .toList();
    }

    public Optional<Zone> find(UUID id) {
        return zoneRepository.findById(id);
    }

    public Zone get(UUID id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone introuvable"));
    }

    // Saisies normalisées en majuscules, comme les adresses envoyées par les apps terrain, pour
    // que le filtre par zone du tableau de bord retrouve bien les ménages correspondants.
    // Une zone = une province, une ville, au moins une commune et au moins un quartier (doublons
    // et lignes vides ignorés).
    public Zone create(String province, String ville, List<String> communes, List<String> quartiers) {
        String p = normalize(province);
        String v = normalize(ville);
        List<String> c = communes == null ? List.of() : communes.stream()
                .map(ZoneService::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (p == null || v == null || c.isEmpty()) {
            throw new IllegalArgumentException("La province, la ville et au moins une commune sont obligatoires");
        }
        List<String> q = normalizeAll(quartiers);
        if (q.isEmpty()) {
            throw new IllegalArgumentException("Choisissez au moins un quartier pour la zone");
        }
        boolean exists = list().stream().anyMatch(z ->
                z.getProvince().equals(p) && z.getVille().equals(v) && z.getCommunes().stream().sorted().toList().equals(c));
        if (exists) {
            throw new IllegalArgumentException("Cette zone existe déjà");
        }
        return zoneRepository.save(new Zone(UUID.randomUUID(), nextCode(p), p, v, c, q));
    }

    // Quartiers d'une zone existante (au moins un) — complète les zones créées avant l'ajout des
    // quartiers, ou ajuste la liste.
    @Transactional
    public Zone updateQuartiers(UUID id, List<String> quartiers) {
        Zone zone = get(id);
        List<String> q = normalizeAll(quartiers);
        if (q.isEmpty()) {
            throw new IllegalArgumentException("Choisissez au moins un quartier pour la zone");
        }
        zone.setQuartiers(q);
        return zone;
    }

    private static List<String> normalizeAll(List<String> values) {
        return values == null ? List.of() : values.stream()
                .map(ZoneService::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    // Code d'une nouvelle zone : numéro de la province sur 2 chiffres (01 = Kinshasa, puis l'ordre
    // alphabétique, cf. DrcProvinces.numberOf), un tiret, puis le premier numéro libre dans cette
    // province sur 4 chiffres (un numéro libéré par une suppression est repris) — 01-0001,
    // 01-0002, 02-0001... Null pour une province hors de la liste de référence.
    String nextCode(String province) {
        String prefix = DrcProvinces.numberOf(province).orElse(null);
        if (prefix == null) {
            return null;
        }
        Set<Integer> used = zoneRepository.findByProvince(province).stream()
                .map(Zone::getCode)
                .filter(code -> code != null && code.matches(Pattern.quote(prefix) + "-\\d{4}"))
                .map(code -> Integer.parseInt(code.substring(prefix.length() + 1)))
                .collect(Collectors.toSet());
        int number = 1;
        while (used.contains(number)) {
            number++;
        }
        return String.format("%s-%04d", prefix, number);
    }

    // Au démarrage : toute zone sans code, ou dont le code ne suit pas la règle actuelle (numéro de
    // sa province + tiret + numéro sur 4 chiffres ; ex. ancien format A1), est renumérotée dans
    // l'ordre de création. Les codes déjà corrects ne
    // bougent pas. Renvoie le nombre de zones renumérotées.
    @Transactional
    public int backfillCodes() {
        List<Zone> toFix = zoneRepository.findAllByOrderByCreatedAtAsc().stream()
                .filter(zone -> !hasValidCode(zone))
                .toList();
        // Libère d'abord les anciens codes pour que leurs numéros puissent être réattribués.
        toFix.forEach(zone -> zone.setCode(null));
        zoneRepository.saveAllAndFlush(toFix);
        for (Zone zone : toFix) {
            zone.setCode(nextCode(zone.getProvince()));
            zoneRepository.saveAndFlush(zone);
        }
        return toFix.size();
    }

    private static boolean hasValidCode(Zone zone) {
        String prefix = DrcProvinces.numberOf(zone.getProvince()).orElse(null);
        return prefix != null && zone.getCode() != null && zone.getCode().matches(Pattern.quote(prefix) + "-\\d{4}");
    }

    // 01-0001 < 01-0002 < 01-0010 < 02-0001 ; zones sans code en dernier.
    private static int sortKey(Zone zone) {
        String code = zone.getCode();
        if (code == null || !code.matches("\\d{2}-\\d{4}")) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(code.substring(0, 2)) * 10_000 + Integer.parseInt(code.substring(3));
    }

    // Les agents affectés à la zone supprimée redeviennent "sans zone" plutôt que d'empêcher la
    // suppression.
    @Transactional
    public void delete(UUID id) {
        Zone zone = get(id);
        agentRepository.findByZone_Id(id).forEach(agent -> agent.setZone(null));
        // Ses sites importés redeviennent « non affectés ».
        zoneSiteService.detachZone(id);
        zoneRepository.delete(zone);
    }

    // Supprime toutes les zones ; les comptes affectés redeviennent "sans zone". Renvoie le
    // nombre de zones supprimées.
    @Transactional
    public int deleteAll() {
        List<Zone> zones = zoneRepository.findAll();
        zones.forEach(zone -> agentRepository.findByZone_Id(zone.getId()).forEach(agent -> agent.setZone(null)));
        zones.forEach(zone -> zoneSiteService.detachZone(zone.getId()));
        zoneRepository.deleteAll(zones);
        return zones.size();
    }

    public long agentCount(UUID zoneId) {
        return agentRepository.countByZone_Id(zoneId);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
