package com.onip.facm01.dashboard;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.household.DrcProvinces;
import com.onip.facm01.household.Household;
import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.household.dto.HouseholdDto;
import com.onip.facm01.zone.Zone;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Historique du SUPERVISEUR : les ménages enregistrés par les agents qui lui sont affectés, avec,
// pour chacun, s'il a été enregistré dans la zone de l'agent ou en dehors.
@Service
public class SupervisorHistoryService {

    public enum ZoneStatus { IN_ZONE, OUT_OF_ZONE, NO_ZONE }

    public record HistoryRow(HouseholdDto household, Agent agent, ZoneStatus zoneStatus) {
        public String zoneLabel() {
            return agent.getZone() == null ? null : agent.getZone().getLabel();
        }
    }

    public record History(List<Agent> agents, List<HistoryRow> rows, long inZone, long outOfZone, long noZone) {
    }

    private final AgentRepository agentRepository;
    private final HouseholdRepository householdRepository;

    public SupervisorHistoryService(AgentRepository agentRepository, HouseholdRepository householdRepository) {
        this.agentRepository = agentRepository;
        this.householdRepository = householdRepository;
    }

    // Les compteurs portent sur tous les enregistrements des agents du superviseur ; les lignes
    // sont filtrées par agent et/ou "hors zone uniquement", les plus récentes en premier.
    @Transactional(readOnly = true)
    public History history(Agent superviseur, UUID agentFilter, boolean outOfZoneOnly) {
        List<Agent> agents = agentRepository.findBySuperviseur_Id(superviseur.getId()).stream()
                .sorted(Comparator.comparing(Agent::getFullName))
                .toList();
        Map<UUID, Agent> byId = agents.stream().collect(Collectors.toMap(Agent::getId, Function.identity()));
        if (byId.isEmpty()) {
            return new History(agents, List.of(), 0, 0, 0);
        }

        List<HistoryRow> all = householdRepository.findByAgent_IdInAndArchivedFalseOrderByCreatedAtDesc(byId.keySet())
                .stream()
                .map(h -> {
                    Agent agent = byId.get(h.getAgent().getId());
                    return new HistoryRow(HouseholdDto.from(h), agent, zoneStatus(h, agent.getZone()));
                })
                .toList();

        long inZone = all.stream().filter(r -> r.zoneStatus() == ZoneStatus.IN_ZONE).count();
        long outOfZone = all.stream().filter(r -> r.zoneStatus() == ZoneStatus.OUT_OF_ZONE).count();
        long noZone = all.size() - inZone - outOfZone;

        List<HistoryRow> rows = all.stream()
                .filter(r -> agentFilter == null || r.agent().getId().equals(agentFilter))
                .filter(r -> !outOfZoneOnly || r.zoneStatus() == ZoneStatus.OUT_OF_ZONE)
                .toList();
        return new History(agents, rows, inZone, outOfZone, noZone);
    }

    // Dans la zone : même province, même ville, et commune parmi celles de la zone (sans tenir
    // compte des majuscules ni des accents). La province du ménage est déduite de sa ville si elle
    // n'a pas été saisie.
    static ZoneStatus zoneStatus(Household household, Zone zone) {
        if (zone == null) {
            return ZoneStatus.NO_ZONE;
        }
        var address = household.getAddress();
        if (address == null) {
            return ZoneStatus.OUT_OF_ZONE;
        }
        String province = DrcProvinces.resolve(address.getProvince(), address.getVille());
        boolean inZone = key(province).equals(key(zone.getProvince()))
                && key(address.getVille()).equals(key(zone.getVille()))
                && zone.getCommunes().stream().anyMatch(c -> key(c).equals(key(address.getCommune())));
        return inZone ? ZoneStatus.IN_ZONE : ZoneStatus.OUT_OF_ZONE;
    }

    private static String key(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();
    }
}
