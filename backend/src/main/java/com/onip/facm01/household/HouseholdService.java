package com.onip.facm01.household;

import com.onip.facm01.agent.Agent;
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

    private static final long MAX_PHOTO_SIZE = 6 * 1024 * 1024; // 6 Mo
    private static final int MAX_PHOTOS = 4;

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
                addressDto.rue(), addressDto.numero(), addressDto.immeuble(), addressDto.etage()));

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
                person.nom(), person.postnom(), person.prenom(), person.dateNaissance(),
                person.sexe(), person.relation());
    }

    // readOnly = true : garde la session Hibernate ouverte le temps du mapping vers HouseholdDto,
    // qui accède à la collection "membres" chargée en LAZY (Household.members). Sans ça
    // (open-in-view=false), l'accès à la collection après le retour du repository lève une
    // LazyInitializationException.
    //
    // Recherche unifiée du tableau de bord admin (nom du chef ou code ménage, commune, plage de
    // dates, actifs/archivés) — tous les paramètres de filtre sont optionnels (null/blank = pas
    // de filtre).
    // Bornes larges substituées à null pour dateFrom/dateTo : PostgreSQL ne peut pas déduire
    // le type d'un paramètre qui n'apparaîtrait que dans un test "IS NULL" (cf. commentaire sur
    // HouseholdRepository.searchAdmin), donc la comparaison de plage est toujours appliquée.
    private static final Instant EPOCH = Instant.EPOCH;
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");

    @Transactional(readOnly = true)
    public Page<HouseholdDto> searchAdmin(
            String search, String ville, String commune, Instant dateFrom, Instant dateTo, boolean archived,
            Pageable pageable) {
        return householdRepository.searchAdmin(
                        search == null ? "" : search,
                        ville == null ? "" : ville,
                        commune == null ? "" : commune,
                        dateFrom == null ? EPOCH : dateFrom,
                        dateTo == null ? FAR_FUTURE : dateTo,
                        archived, pageable)
                .map(HouseholdDto::from);
    }

    @Transactional(readOnly = true)
    public List<String> distinctCommunes(String ville) {
        return householdRepository.findDistinctCommunes(ville == null ? "" : ville);
    }

    @Transactional(readOnly = true)
    public List<String> distinctVilles(String commune) {
        return householdRepository.findDistinctVilles(commune == null ? "" : commune);
    }

    @Transactional(readOnly = true)
    public HouseholdDto get(UUID id) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        return HouseholdDto.from(household);
    }

    // Un ménage recensé ne se supprime jamais depuis le tableau de bord — seulement archivé
    // (réversible via restore()). Voir le commentaire sur Household.archived.
    @Transactional
    public void archive(UUID id) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        household.setArchived(true);
        household.setArchivedAt(Instant.now());
        householdRepository.save(household);
    }

    @Transactional
    public void restore(UUID id) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        household.setArchived(false);
        household.setArchivedAt(null);
        householdRepository.save(household);
    }

    // Variantes utilisées par l'API REST (/api/households), accessible aux comptes AGENT en plus
    // des ADMIN : un agent ne doit voir/modifier que ses propres ménages, un admin voit tout.
    // Les routes Thymeleaf /dashboard/** au-dessus utilisent les méthodes sans scope, car déjà
    // réservées aux ADMIN par la config de sécurité — pas besoin d'y dupliquer la vérification.

    @Transactional(readOnly = true)
    public Page<HouseholdDto> list(Pageable pageable, Agent currentAgent) {
        Page<Household> page = currentAgent.getRole().isAdminTier()
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
        if (!currentAgent.getRole().isAdminTier()
                && (household.getAgent() == null || !household.getAgent().getId().equals(currentAgent.getId()))) {
            throw new IllegalArgumentException("Ménage introuvable : " + household.getId());
        }
    }

    // Remplace l'intégralité des photos du ménage par ce nouveau jeu (l'app terrain envoie à
    // chaque enregistrement toutes les photos actuellement capturées sur l'appareil — c'est plus
    // simple et plus sûr qu'un ajout incrémental, qui accumulerait des doublons à chaque nouvelle
    // synchronisation du même ménage édité).
    @Transactional
    public void replacePhotos(UUID id, List<PhotoUpload> photos, Agent currentAgent) {
        if (photos.size() > MAX_PHOTOS) {
            throw new IllegalArgumentException("Maximum " + MAX_PHOTOS + " photos par ménage");
        }
        for (PhotoUpload upload : photos) {
            if (upload.bytes().length == 0) {
                throw new IllegalArgumentException("Le fichier envoyé est vide");
            }
            if (upload.bytes().length > MAX_PHOTO_SIZE) {
                throw new IllegalArgumentException("Une photo dépasse la taille maximale autorisée (6 Mo)");
            }
            if (upload.contentType() == null || !upload.contentType().startsWith("image/")) {
                throw new IllegalArgumentException("Le fichier doit être une image");
            }
        }

        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        requireAccess(household, currentAgent);

        householdPhotoRepository.deleteByHousehold_Id(id);
        List<HouseholdPhoto> entities = new ArrayList<>();
        for (int i = 0; i < photos.size(); i++) {
            PhotoUpload upload = photos.get(i);
            entities.add(new HouseholdPhoto(household, i, upload.bytes(), upload.contentType()));
        }
        householdPhotoRepository.saveAll(entities);

        household.setPhotoCount(photos.size());
        householdRepository.save(household);
    }

    @Transactional(readOnly = true)
    public HouseholdPhoto getPhoto(UUID id, int position, Agent currentAgent) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        requireAccess(household, currentAgent);
        return findPhotoByPosition(id, position);
    }

    // Variante sans scope agent, pour la page de détail du tableau de bord admin (déjà réservée
    // aux ADMIN/SUPER_ADMIN par la config de sécurité de /dashboard/**).
    @Transactional(readOnly = true)
    public HouseholdPhoto getPhoto(UUID id, int position) {
        return findPhotoByPosition(id, position);
    }

    private HouseholdPhoto findPhotoByPosition(UUID id, int position) {
        return householdPhotoRepository.findByHousehold_IdOrderByPosition(id).stream()
                .filter(p -> p.getPosition() == position)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Aucune photo à cette position pour ce ménage : " + id));
    }
}
