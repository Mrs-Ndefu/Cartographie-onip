package com.onip.facm01.household;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRole;
import com.onip.facm01.household.dto.AddressDto;
import com.onip.facm01.household.dto.GeoLocationDto;
import com.onip.facm01.household.dto.HouseholdDto;
import com.onip.facm01.household.dto.HouseholdEditForm;
import com.onip.facm01.household.dto.HouseholdSyncItem;
import com.onip.facm01.household.dto.PersonDto;
import com.onip.facm01.household.dto.SyncRejection;
import com.onip.facm01.household.dto.SyncResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class HouseholdService {

    private static final long MAX_PHOTO_SIZE = 6 * 1024 * 1024; // 6 Mo
    private static final int MAX_PHOTOS = 4;

    private final HouseholdRepository householdRepository;
    private final HouseholdPhotoRepository householdPhotoRepository;
    private final HouseholdModificationRepository householdModificationRepository;

    public HouseholdService(
            HouseholdRepository householdRepository,
            HouseholdPhotoRepository householdPhotoRepository,
            HouseholdModificationRepository householdModificationRepository) {
        this.householdRepository = householdRepository;
        this.householdPhotoRepository = householdPhotoRepository;
        this.householdModificationRepository = householdModificationRepository;
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
                DrcProvinces.resolve(addressDto.province(), addressDto.ville()),
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
    // Recherche unifiée du tableau de bord (cf. HouseholdFilter) — tous les filtres sont
    // optionnels (null/blank = pas de filtre).
    // Bornes larges substituées à null pour dateFrom/dateTo : PostgreSQL ne peut pas déduire
    // le type d'un paramètre qui n'apparaîtrait que dans un test "IS NULL" (cf. commentaire sur
    // HouseholdRepository.searchAdmin), donc la comparaison de plage est toujours appliquée.
    private static final Instant EPOCH = Instant.EPOCH;
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");
    private static final int MAX_MAP_POINTS = 2000;

    @Transactional(readOnly = true)
    public Page<HouseholdDto> searchAdmin(HouseholdFilter filter, boolean archived, Pageable pageable) {
        return householdRepository.searchAdmin(
                        orEmpty(filter.search()),
                        orEmpty(filter.province()),
                        orEmpty(filter.ville()),
                        orEmpty(filter.commune()),
                        orEmpty(filter.quartier()),
                        orEmpty(filter.statut()),
                        filter.dateFrom() == null ? EPOCH : filter.dateFrom(),
                        filter.dateTo() == null ? FAR_FUTURE : filter.dateTo(),
                        archived, pageable)
                .map(HouseholdDto::from);
    }

    // Points GPS de tous les ménages correspondant aux filtres (pas seulement la page affichée),
    // pour que la carte montre et cadre toute la zone filtrée. Plafonné pour garder la page
    // légère.
    @Transactional(readOnly = true)
    public List<MapPoint> mapPoints(HouseholdFilter filter) {
        return searchAdmin(filter, false, PageRequest.of(0, MAX_MAP_POINTS)).stream()
                .filter(h -> h.location() != null && h.location().latitude() != null && h.location().longitude() != null)
                .map(h -> new MapPoint(
                        h.location().latitude(),
                        h.location().longitude(),
                        h.codeMenage(),
                        h.chef() == null ? "" : String.join(" ", Stream.of(h.chef().prenom(), h.chef().nom())
                                .filter(v -> v != null && !v.isBlank()).toList())))
                .toList();
    }

    public record MapPoint(double latitude, double longitude, String codeMenage, String chef) {
    }

    @Transactional(readOnly = true)
    public List<String> distinctProvinces() {
        return householdRepository.findDistinctProvinces();
    }

    @Transactional(readOnly = true)
    public List<String> distinctVilles(String province, String commune) {
        return householdRepository.findDistinctVilles(orEmpty(province), orEmpty(commune));
    }

    @Transactional(readOnly = true)
    public List<String> distinctCommunes(String province, String ville) {
        return householdRepository.findDistinctCommunes(orEmpty(province), orEmpty(ville));
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    // Complète la province des ménages enregistrés avant son ajout, à partir de la ville.
    // Renvoie le nombre de ménages mis à jour.
    @Transactional
    public int backfillProvinces() {
        int updated = 0;
        for (Household household : householdRepository.findWithoutProvince()) {
            String province = DrcProvinces.fromVille(household.getAddress().getVille()).orElse(null);
            if (province != null) {
                household.getAddress().setProvince(province);
                updated++;
            }
        }
        return updated;
    }

    // Modification d'un ménage depuis le tableau de bord (ADMIN, SUPERVISEUR — cf.
    // SecurityConfig), avec un motif obligatoire conservé dans l'historique du ménage. Le code
    // ménage, le GPS et l'agent d'origine ne changent pas. updatedAt avance : une synchronisation
    // ultérieure d'une version plus ancienne depuis la tablette n'écrasera donc pas cette
    // correction (cf. upsert).
    @Transactional
    public void updateFromDashboard(UUID id, HouseholdEditForm form, Agent author) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ménage introuvable : " + id));
        if (isBlank(form.getMotif())) {
            throw new IllegalArgumentException("Le motif de la modification est obligatoire");
        }
        if (isBlank(form.getChefNom())) {
            throw new IllegalArgumentException("Le nom du chef de ménage est obligatoire");
        }
        if (isBlank(form.getCommune()) || isBlank(form.getQuartier())) {
            throw new IllegalArgumentException("La commune et le quartier sont obligatoires");
        }
        long keptMembers = form.getMembres().stream().filter(m -> !m.isRemoved() && !m.isBlank()).count();
        if (form.getNombreMembres() != null && form.getNombreMembres() != 1 + keptMembers) {
            throw new IllegalArgumentException("Le nombre de membres déclaré (" + form.getNombreMembres()
                    + ") ne correspond pas aux fiches : chef + " + keptMembers + " membre(s) renseigné(s) = "
                    + (1 + keptMembers) + ". Complétez ou retirez des fiches membres.");
        }

        household.setAddress(new AddressEmbeddable(
                DrcProvinces.resolve(form.getProvince(), form.getVille()),
                upper(form.getVille()), upper(form.getCommune()), upper(form.getQuartier()),
                upper(form.getRue()), upper(form.getNumero()), upper(form.getImmeuble()), upper(form.getEtage())));
        if (form.getStatus() != null) {
            household.setStatus(form.getStatus());
        }

        List<HouseholdMember> members = new ArrayList<>();
        members.add(new HouseholdMember(
                UUID.randomUUID(), null, true, -1,
                upper(form.getChefNom()), upper(form.getChefPostnom()), upper(form.getChefPrenom()),
                trimToNull(form.getChefDateNaissance()), form.getChefSexe(), null));
        int position = 0;
        for (HouseholdEditForm.MemberForm membre : form.getMembres()) {
            if (membre.isRemoved() || membre.isBlank()) {
                continue;
            }
            members.add(new HouseholdMember(
                    UUID.randomUUID(), null, false, position++,
                    upper(membre.getNom()), upper(membre.getPostnom()), upper(membre.getPrenom()),
                    trimToNull(membre.getDateNaissance()), membre.getSexe(), upper(membre.getRelation())));
        }
        household.replaceMembers(members);
        household.setNombreMembresDeclare(members.size());
        household.setUpdatedAt(Instant.now());
        householdRepository.save(household);
        householdModificationRepository.save(new HouseholdModification(
                household, author.getUsername(), author.getFullName(), author.getRole(), form.getMotif().trim()));
    }

    @Transactional(readOnly = true)
    public List<HouseholdModification> modifications(UUID householdId) {
        return householdModificationRepository.findByHousehold_IdOrderByModifiedAtDesc(householdId);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    // Même convention que les apps terrain : tout le texte saisi est en majuscules.
    private static String upper(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase();
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
        // Le SUPER_ADMIN voit tous les ménages mais ne pose aucune action dessus.
        if (currentAgent.getRole() == AgentRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Le SUPER_ADMIN ne peut pas supprimer de ménage");
        }
        householdModificationRepository.deleteAll(householdModificationRepository.findByHousehold_IdOrderByModifiedAtDesc(id));
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
