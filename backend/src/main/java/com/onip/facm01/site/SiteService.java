package com.onip.facm01.site;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRole;
import com.onip.facm01.household.dto.SyncRejection;
import com.onip.facm01.household.dto.SyncResponse;
import com.onip.facm01.site.dto.SiteDto;
import com.onip.facm01.site.dto.SiteSyncItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SiteService {

    private final SiteRepository siteRepository;

    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    // Le repérage de site est réservé au CHEF_EQUIPE (et aux rôles admin-tier/superviseur) — un
    // AGENT n'a accès qu'à l'adressage des ménages, cf. HomeScreen côté Android qui ne lui
    // affiche même pas l'écran. Vérifié aussi côté serveur : une ancienne version de l'app, ou un
    // appel direct à l'API, ne doit pas pouvoir contourner cette restriction.
    @Transactional
    public SyncResponse sync(List<SiteSyncItem> items, Agent agent) {
        if (agent != null && agent.getRole() == AgentRole.AGENT) {
            List<SyncRejection> rejected = items.stream()
                    .map(item -> new SyncRejection(item.id(), "Les agents n'ont pas accès au repérage de site."))
                    .toList();
            return new SyncResponse(List.of(), rejected);
        }

        List<UUID> accepted = new ArrayList<>();
        List<SyncRejection> rejected = new ArrayList<>();

        for (SiteSyncItem item : items) {
            try {
                upsert(item, agent);
                accepted.add(item.id());
            } catch (Exception e) {
                rejected.add(new SyncRejection(item.id(), e.getMessage()));
            }
        }

        return new SyncResponse(accepted, rejected);
    }

    private void upsert(SiteSyncItem item, Agent agent) {
        Site site = siteRepository.findById(item.id()).orElse(null);
        Instant now = Instant.now();

        if (site == null) {
            site = new Site(item.id());
            site.setCreatedAt(item.createdAt() != null ? item.createdAt() : now);
            site.setAgent(agent);
        } else if (site.getUpdatedAt() != null && item.updatedAt() != null
                && !item.updatedAt().isAfter(site.getUpdatedAt())) {
            // Version reçue pas plus récente que celle déjà stockée : on acquitte sans écraser
            // (même principe que HouseholdService.upsert).
            site.setSyncedAt(now);
            siteRepository.save(site);
            return;
        }

        site.setProvince(item.province());
        site.setVille(item.ville());
        site.setCommune(item.commune());
        site.setQuartierGroupement(item.quartierGroupement());
        site.setVillageAvenueRue(item.villageAvenueRue());
        site.setNomSite(item.nomSite());
        site.setNumeroTelephone(item.numeroTelephone());
        site.setSerieTelephoneGps(item.serieTelephoneGps());
        site.setLatitude(item.latitude());
        site.setLongitude(item.longitude());
        site.setAltitude(item.altitude());
        site.setGpsPrecision(item.gpsPrecision());
        site.setTypeSite(item.typeSite());
        site.setTypeSiteAutre(item.typeSiteAutre());
        site.setProprieteStatut(item.proprieteStatut());
        site.setOptionOccupation(item.optionOccupation());
        site.setDisponibiliteImmediate(item.disponibiliteImmediate());
        site.setDelaiEstime(item.delaiEstime());
        site.setProprietaireNomContact(item.proprietaireNomContact());
        site.setTypeInfrastructure(item.typeInfrastructure());
        site.setTypeInfrastructureAutre(item.typeInfrastructureAutre());
        site.setSuperficieBatimentM2(item.superficieBatimentM2());
        site.setSuperficieTerrainM2(item.superficieTerrainM2());
        site.setNombrePieces(item.nombrePieces());
        site.setEtatBatiment(item.etatBatiment());
        site.setEtatBatimentObservations(item.etatBatimentObservations());
        site.setAccesRoutier(item.accesRoutier());
        site.setRaccordeSnel(item.raccordeSnel());
        site.setStabiliteSnel(item.stabiliteSnel());
        site.setGroupeElectrogeneSolaire(item.groupeElectrogeneSolaire());
        site.setNonElectrifie(item.nonElectrifie());
        site.setRegideso(item.regideso());
        site.setSanitaires(item.sanitaires());
        site.setConnectiviteInternet(item.connectiviteInternet());
        site.setReseauxDisponibles(item.reseauxDisponibles());
        site.setClotureSecurise(item.clotureSecurise());
        site.setProximitePolice(item.proximitePolice());
        site.setDistancePoliceMetres(item.distancePoliceMetres());
        site.setAvisGlobal(item.avisGlobal());
        site.setTravauxAmenagement(item.travauxAmenagement());
        site.setMotifDefavorable(item.motifDefavorable());
        site.setObservationsParticulieres(item.observationsParticulieres());
        site.setNomSignatureEnqueteur(item.nomSignatureEnqueteur());
        site.setDateEvaluation(item.dateEvaluation());
        site.setUpdatedAt(item.updatedAt() != null ? item.updatedAt() : now);
        site.setSyncedAt(now);

        siteRepository.save(site);
    }

    // Tableau de bord (ADMIN/SUPER_ADMIN/SUPERVISEUR, cf. SecurityConfig) : toutes les fiches,
    // sans scope agent, avec recherche — même principe que HouseholdService.searchAdmin.
    @Transactional(readOnly = true)
    public Page<SiteDto> listAdmin(String search, Pageable pageable) {
        return siteRepository.searchAdmin(search == null ? "" : search, pageable).map(SiteDto::from);
    }

    @Transactional(readOnly = true)
    public SiteDto get(UUID id) {
        return SiteDto.from(siteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fiche de repérage introuvable : " + id)));
    }

    // Même principe que HouseholdService.list/get/delete : un agent ne voit/modifie que ses
    // propres fiches, un admin-tier voit tout.

    @Transactional(readOnly = true)
    public Page<SiteDto> list(Pageable pageable, Agent currentAgent) {
        Page<Site> page = currentAgent.getRole().isAdminTier()
                ? siteRepository.findAll(pageable)
                : siteRepository.findByAgent_Id(currentAgent.getId(), pageable);
        return page.map(SiteDto::from);
    }

    @Transactional(readOnly = true)
    public SiteDto get(UUID id, Agent currentAgent) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fiche de repérage introuvable : " + id));
        requireAccess(site, currentAgent);
        return SiteDto.from(site);
    }

    @Transactional
    public void delete(UUID id, Agent currentAgent) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fiche de repérage introuvable : " + id));
        requireAccess(site, currentAgent);
        siteRepository.deleteById(id);
    }

    private void requireAccess(Site site, Agent currentAgent) {
        if (!currentAgent.getRole().isAdminTier()
                && (site.getAgent() == null || !site.getAgent().getId().equals(currentAgent.getId()))) {
            throw new IllegalArgumentException("Fiche de repérage introuvable : " + site.getId());
        }
    }
}
