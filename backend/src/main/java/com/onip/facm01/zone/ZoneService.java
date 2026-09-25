package com.onip.facm01.zone;

import com.onip.facm01.agent.AgentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        return zoneRepository.findAllByOrderByProvinceAscVilleAscCommuneAscQuartierAsc();
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
    public Zone create(String province, String ville, String commune, String quartier) {
        String p = normalize(province);
        String v = normalize(ville);
        String c = normalize(commune);
        String q = normalize(quartier);
        if (p == null || v == null || c == null) {
            throw new IllegalArgumentException("La province, la ville et la commune sont obligatoires");
        }
        if (zoneRepository.existsByProvinceAndVilleAndCommuneAndQuartier(p, v, c, q)) {
            throw new IllegalArgumentException("Cette zone existe déjà");
        }
        return zoneRepository.save(new Zone(UUID.randomUUID(), p, v, c, q));
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
