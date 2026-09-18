package com.onip.facm01.household;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRole;
import com.onip.facm01.household.dto.AddressDto;
import com.onip.facm01.household.dto.GeoLocationDto;
import com.onip.facm01.household.dto.HouseholdDto;
import com.onip.facm01.household.dto.HouseholdSyncItem;
import com.onip.facm01.household.dto.PersonDto;
import com.onip.facm01.household.dto.SyncRejection;
import com.onip.facm01.household.dto.SyncResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class HouseholdService {

    private static final long MAX_PHOTO_SIZE = 2 * 1024 * 1024; // 2 Mo

    private final HouseholdRepository householdRepository;
    private final HouseholdPhotoRepository householdPhotoRepository;

    public HouseholdService(HouseholdRepository householdRepository, HouseholdPhotoRepository householdPhotoRepository) {
        this.householdRepository = householdRepository;
        this.householdPhotoRepository = householdPhotoRepository;
    }

    @Transactional
    public SyncResponse sync(List<HouseholdSyncItem> items, Agent agent) {
        List<UUID> accepted = new ArrayList<>();
        List<SyncRejection> rejected = new ArrayList<>();

        for (HouseholdSyncItem item : items) {
            try {
                upsert(item, agent);
                accepted.add(item.id());
            } catch (Exception e) {
                rejected.add(new SyncRejection(item.id(), e.getMessage()));
            }
        }

        return new SyncResponse(accepted, rejected);
    }

    private void upsert(HouseholdSyncItem item, Agent agent) {
        Household household = householdRepository.findById(item.id()).orElse(null);
        Instant now = Instant.now();

        if (household == null) {
            household = new Household(item.id());
            household.setCreatedAt(item.createdAt() != null ? item.createdAt() : now);
            household.setAgent(agent);
        } else if (household.getUpdatedAt() != null && item.updatedAt() != null
                && !item.updatedAt().isAfter(household.getUpdatedAt())) {
            // La version reçue n'est pas plus récente que celle déjà stockée : on ne fait
            // qu'acquitter la synchronisation sans écraser des changements plus récents.
            household.setSyncedAt(now);
            householdRepository.save(household);
            return;
        }

        household.setCodeMenage(item.codeMenage());
        household.setNombreMembresDeclare(item.nombreMembres());
        household.setStatus(item.status());
        household.setUpdatedAt(item.updatedAt() != null ? item.updatedAt() : now);
        household.setSyncedAt(now);

        AddressDto addressDto = item.address();
        household.setAddress(new AddressEmbeddable(
                addressDto.ville(), addressDto.commune(), addressDto.quartier(),
                addressDto.rue(), addressDto.numero(), addressDto.immeuble()));

        GeoLocationDto locationDto = item.location();
        household.setLocation(locationDto == null ? null : new GeoLocationEmbeddable(
                locationDto.latitude(), locationDto.longitude(), locationDto.precision(), locationDto.saisieManuelle()));

        household.setMeta(new FormMetaEmbeddable(
                item.meta().faitA(), item.meta().dateEncodage(), item.meta().nombreFiches(),
                item.meta().agentCarthographe(), item.meta().renseignant()));

        List<HouseholdMember> members = new ArrayList<>();
        members.add(toEntity(item.chef(), true, -1));
        List<PersonDto> membres = item.membres() != null ? item.membres() : List.of();
        for (int i = 0; i < membres.size(); i++) {
            members.add(toEntity(membres.get(i), false, i));
        }
        household.replaceMembers(members);

        householdRepository.save(household);
    }

    private HouseholdMember toEntity(PersonDto person, boolean chef, int position) {
        return new HouseholdMember(
                UUID.randomUUID(), null, chef, position,
                person.nom(), person.postnom(), person.prenom(), person.dateNaissance(), person.sexe(),
                person.relation());
    }

    // readOnly = true : garde la session Hibernate ouverte le temps du mapping vers HouseholdDto,
    // qui accède à la collection "membres" chargée en LAZY (Household.members). Sans ça
    // (open-in-view=false), l'accès à la collection après le retour du repository lève une
    // LazyInitializationException.
    @Transactional(readOnly = true)
    public Page<HouseholdDto> list(Pageable pageable) {
        return householdRepository.findAll(pageable).map(HouseholdDto::from);
    }

    @Transactional(readOnly = true)
    public Page<HouseholdDto> search(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return list(pageable);
        }
        return householdRepository.searchByChefName(search.trim(), pageable).map(HouseholdDto::from);
    }

    @Transactional(readOnly = true)
    public HouseholdDto get(UUID id) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        return HouseholdDto.from(household);
    }

    @Transactional
    public void delete(UUID id) {
        if (!householdRepository.existsById(id)) {
            throw new IllegalArgumentException("Ménage introuvable : " + id);
        }
        householdRepository.deleteById(id);
    }

    // Variantes utilisées par l'API REST (/api/households), accessible aux comptes AGENT en plus
    // des ADMIN : un agent ne doit voir/modifier que ses propres ménages, un admin voit tout.
    // Les routes Thymeleaf /dashboard/** au-dessus utilisent les méthodes sans scope, car déjà
    // réservées aux ADMIN par la config de sécurité — pas besoin d'y dupliquer la vérification.

    @Transactional(readOnly = true)
    public Page<HouseholdDto> list(Pageable pageable, Agent currentAgent) {
        Page<Household> page = currentAgent.getRole() == AgentRole.ADMIN
                ? householdRepository.findAll(pageable)
                : householdRepository.findByAgent_Id(currentAgent.getId(), pageable);
        return page.map(HouseholdDto::from);
    }

    @Transactional(readOnly = true)
    public HouseholdDto get(UUID id, Agent currentAgent) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        requireAccess(household, currentAgent);
        return HouseholdDto.from(household);
    }

    @Transactional
    public void delete(UUID id, Agent currentAgent) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        requireAccess(household, currentAgent);
        householdRepository.deleteById(id);
    }

    private void requireAccess(Household household, Agent currentAgent) {
        if (currentAgent.getRole() != AgentRole.ADMIN
                && (household.getAgent() == null || !household.getAgent().getId().equals(currentAgent.getId()))) {
            throw new IllegalArgumentException("Ménage introuvable : " + household.getId());
        }
    }

    @Transactional
    public void attachPhoto(UUID id, byte[] photo, String contentType, Agent currentAgent) {
        if (photo.length == 0) {
            throw new IllegalArgumentException("Le fichier envoyé est vide");
        }
        if (photo.length > MAX_PHOTO_SIZE) {
            throw new IllegalArgumentException("La photo dépasse la taille maximale autorisée (2 Mo)");
        }
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit être une image");
        }

        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        requireAccess(household, currentAgent);

        HouseholdPhoto existing = householdPhotoRepository.findById(id).orElse(null);
        if (existing != null) {
            existing.update(photo, contentType);
            householdPhotoRepository.save(existing);
        } else {
            householdPhotoRepository.save(new HouseholdPhoto(id, photo, contentType));
        }

        household.setHasPhoto(true);
        householdRepository.save(household);
    }

    @Transactional(readOnly = true)
    public HouseholdPhoto getPhoto(UUID id, Agent currentAgent) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        requireAccess(household, currentAgent);
        return householdPhotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aucune photo pour ce ménage : " + id));
    }
}
