package com.onip.facm01.zone;

import com.onip.facm01.agent.AgentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final AgentRepository agentRepository;

    public ZoneService(ZoneRepository zoneRepository, AgentRepository agentRepository) {
        this.zoneRepository = zoneRepository;
        this.agentRepository = agentRepository;
    }

    public List<Zone> list() {
        return zoneRepository.findAllByOrderByProvinceAscVilleAsc();
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
    // Une zone = une province, une ville et au moins une commune (doublons et lignes vides
    // ignorés).
    public Zone create(String province, String ville, List<String> communes) {
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
        boolean exists = list().stream().anyMatch(z ->
                z.getProvince().equals(p) && z.getVille().equals(v) && z.getCommunes().stream().sorted().toList().equals(c));
        if (exists) {
            throw new IllegalArgumentException("Cette zone existe déjà");
        }
        return zoneRepository.save(new Zone(UUID.randomUUID(), p, v, c));
    }

    // Les agents affectés à la zone supprimée redeviennent "sans zone" plutôt que d'empêcher la
    // suppression.
    @Transactional
    public void delete(UUID id) {
        Zone zone = get(id);
        agentRepository.findByZone_Id(id).forEach(agent -> agent.setZone(null));
        zoneRepository.delete(zone);
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
