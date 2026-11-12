package com.onip.facm01.zone;

import com.onip.facm01.household.DrcProvinces;
import com.onip.facm01.site.SiteRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

// Import Excel des sites (page Zones > « Ajout site ») et affectation aux zones.
//
// Fichier attendu (.xlsx, 1re feuille) : colonnes Nom du site, Adresse, Province, Ville,
// Territoire. Si la 1re ligne est un en-tête, les colonnes sont reconnues par leur intitulé
// (dans n'importe quel ordre) ; sinon elles sont lues dans cet ordre (A à E).
//
// L'import AJOUTE les sites à la liste existante ; une ligne identique à un site déjà présent
// (même nom, province, ville et territoire) est ignorée, ce qui permet de réimporter un fichier
// complété sans créer de doublons.
@Service
public class ZoneSiteService {

    private final ZoneSiteRepository zoneSiteRepository;
    private final ZoneRepository zoneRepository;
    private final SiteRepository siteRepository;

    public ZoneSiteService(ZoneSiteRepository zoneSiteRepository, ZoneRepository zoneRepository, SiteRepository siteRepository) {
        this.zoneSiteRepository = zoneSiteRepository;
        this.zoneRepository = zoneRepository;
        this.siteRepository = siteRepository;
    }

    public record ImportResult(int imported, int duplicates, int assigned, int unassigned) {
    }

    @Transactional(readOnly = true)
    public Page<ZoneSite> search(String zoneFilter, UUID zoneId, String search, Pageable pageable) {
        return zoneSiteRepository.search(zoneFilter, zoneId, search == null ? "" : search.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Long> countsByZone() {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : zoneSiteRepository.countByZone()) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public long countUnassigned() {
        return zoneSiteRepository.countByZoneIsNull();
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return zoneSiteRepository.count();
    }

    // Site tel que proposé dans l'app mobile (fiche de repérage) : le choix du nom remplit d'office
    // province, ville/territoire, commune et adresse.
    // visite = une fiche de repérage (non retirée) porte déjà ce nom de site dans cette province ;
    // visiteComplete = cette fiche a tous ses champs obligatoires (cf. Site.champsManquants).
    public record ZoneSiteOption(String id, String nomSite, String adresse, String province,
                                 String villeTerritoire, String commune, String zoneCode,
                                 boolean visite, boolean visiteComplete) {
    }

    // Sites de la zone d'un agent (vide s'il n'a pas de zone), triés par nom. Province et
    // ville/territoire sont ceux de la zone : c'est la référence géographique de l'affectation.
    @Transactional(readOnly = true)
    public List<ZoneSiteOption> optionsForZone(Zone zone) {
        if (zone == null) {
            return List.of();
        }
        Map<String, Boolean> visits = visits();
        return zoneSiteRepository.findByZone_Id(zone.getId()).stream()
                .sorted(java.util.Comparator.comparing(s -> key(s.getNom())))
                .map(s -> {
                    Boolean complete = visitOf(s.getNom(), zone.getProvince(), visits);
                    return new ZoneSiteOption(
                            s.getId().toString(),
                            s.getNom(),
                            s.getAdresse(),
                            zone.getProvince(),
                            zone.getVille(),
                            communeOf(s, zone),
                            zone.getCode(),
                            complete != null,
                            Boolean.TRUE.equals(complete));
                })
                .toList();
    }

    // Fiches de repérage actives indexées "NOM|PROVINCE" (une fiche sans province donne "NOM|",
    // reconnue au seul nom du site) -> true si au moins une de ces fiches est complète.
    private Map<String, Boolean> visits() {
        Map<String, Boolean> visits = new HashMap<>();
        for (com.onip.facm01.site.Site fiche : siteRepository.findByArchivedFalseAndNomSiteIsNotNull()) {
            visits.merge(key(fiche.getNomSite()) + "|" + key(fiche.getProvince()),
                    fiche.champsManquants().isEmpty(), Boolean::logicalOr);
        }
        return visits;
    }

    // null = pas visité ; true = visité, fiche complète ; false = visité, fiche incomplète.
    private static Boolean visitOf(String nom, String province, Map<String, Boolean> visits) {
        Boolean withProvince = visits.get(key(nom) + "|" + key(province));
        Boolean withoutProvince = visits.get(key(nom) + "|");
        if (withProvince == null) return withoutProvince;
        if (withoutProvince == null) return withProvince;
        return withProvince || withoutProvince;
    }

    // Statut de visite des sites donnés (colonne Statut de la page Sites) : absent = à visiter,
    // true = visité complet, false = visité incomplet.
    @Transactional(readOnly = true)
    public Map<UUID, Boolean> visitStatus(List<ZoneSite> sites) {
        Map<String, Boolean> visits = visits();
        Map<UUID, Boolean> status = new HashMap<>();
        for (ZoneSite s : sites) {
            String province = s.getZone() != null ? s.getZone().getProvince() : s.getProvince();
            Boolean complete = visitOf(s.getNom(), province, visits);
            if (complete != null) {
                status.put(s.getId(), complete);
            }
        }
        return status;
    }

    // Commune de la zone correspondant au site : la seule si la zone n'en a qu'une, sinon celle
    // citée dans le territoire, la ville ou l'adresse du site (le nom de la ville ne compte qu'en
    // dernier recours, cf. matchZone). Null si on ne peut pas trancher.
    static String communeOf(ZoneSite site, Zone zone) {
        List<String> communes = zone.getCommunes();
        String own = key(site.getCommune());
        if (!own.isEmpty()) {
            for (String c : communes) {
                if (key(c).equals(own)) {
                    return c;
                }
            }
        }
        if (communes.size() == 1) {
            return communes.get(0);
        }
        String territoire = key(site.getTerritoire());
        String ville = key(site.getVille());
        String villeKey = key(zone.getVille());
        for (String c : communes) {
            String k = key(c);
            if (!k.equals(villeKey) && (k.equals(territoire) || k.equals(ville))) {
                return c;
            }
        }
        String text = " " + key(site.getAdresse()) + " ";
        List<String> found = communes.stream()
                .filter(c -> !key(c).equals(villeKey))
                .filter(c -> text.matches(".*[^A-Z0-9]" + Pattern.quote(key(c)) + "[^A-Z0-9].*"))
                .toList();
        if (found.size() == 1) {
            return found.get(0);
        }
        if (found.isEmpty()) {
            return communes.stream()
                    .filter(c -> key(c).equals(villeKey) && (text.contains(villeKey) || territoire.equals(villeKey)))
                    .findFirst().orElse(null);
        }
        return null;
    }

    @Transactional
    public ImportResult importFrom(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choisissez un fichier Excel (.xlsx) à importer");
        }
        List<ZoneSite> parsed = parse(file);
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucun site trouvé dans le fichier (colonnes attendues : nom du site, adresse, province, ville, territoire)");
        }
        Set<String> existing = new HashSet<>();
        zoneSiteRepository.findAll().forEach(s -> existing.add(identity(s)));
        List<Zone> zones = zoneRepository.findAll();

        List<ZoneSite> toSave = new ArrayList<>();
        int duplicates = 0;
        int assigned = 0;
        for (ZoneSite site : parsed) {
            if (!existing.add(identity(site))) {
                duplicates++;
                continue;
            }
            Zone zone = matchZone(site, zones);
            site.setZone(zone);
            if (zone != null) {
                assigned++;
            }
            toSave.add(site);
        }
        zoneSiteRepository.saveAll(toSave);
        return new ImportResult(toSave.size(), duplicates, assigned, toSave.size() - assigned);
    }

    // Relance l'affectation automatique des sites encore non affectés (utile après avoir créé de
    // nouvelles zones). Retourne le nombre de sites nouvellement affectés.
    @Transactional
    public int autoAssignUnassigned() {
        List<Zone> zones = zoneRepository.findAll();
        int count = 0;
        for (ZoneSite site : zoneSiteRepository.findByZoneIsNull()) {
            Zone zone = matchZone(site, zones);
            if (zone != null) {
                site.setZone(zone);
                site.setZoneManuelle(false);
                count++;
            }
        }
        return count;
    }

    // Affectation manuelle par l'ADMIN ; zoneId null = retirer le site de sa zone.
    @Transactional
    public ZoneSite assign(UUID siteId, UUID zoneId) {
        ZoneSite site = get(siteId);
        if (zoneId == null) {
            site.setZone(null);
            site.setZoneManuelle(true);
        } else {
            Zone zone = zoneRepository.findById(zoneId)
                    .orElseThrow(() -> new IllegalArgumentException("Zone introuvable"));
            site.setZone(zone);
            site.setZoneManuelle(true);
        }
        return site;
    }

    @Transactional(readOnly = true)
    public ZoneSite find(UUID siteId) {
        return get(siteId);
    }

    // Modification d'un site par l'ADMIN. Si sa zone avait été trouvée automatiquement (ou s'il
    // n'en avait pas), elle est recalculée d'après les nouvelles valeurs ; une zone choisie à la
    // main est conservée.
    @Transactional
    // Le formulaire propose Province -> Ville/Territoire -> Commune en listes enchaînées : la
    // ville/territoire choisie est enregistrée comme ville ET territoire du site.
    public ZoneSite update(UUID siteId, String nom, String adresse, String province, String villeTerritoire, String commune) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du site est obligatoire");
        }
        ZoneSite site = get(siteId);
        String v = upperOrNull(clean(villeTerritoire));
        String p = clean(province);
        site.update(
                clean(nom),
                emptyToNull(clean(adresse)),
                p == null ? DrcProvinces.resolve(null, v) : DrcProvinces.normalize(p),
                v,
                v,
                upperOrNull(clean(commune)));
        if (!site.isZoneManuelle()) {
            site.setZone(matchZone(site, zoneRepository.findAll()));
        }
        return site;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }

    @Transactional
    public String delete(UUID siteId) {
        ZoneSite site = get(siteId);
        zoneSiteRepository.delete(site);
        return site.getNom();
    }

    @Transactional
    public int deleteAll() {
        int count = (int) zoneSiteRepository.count();
        zoneSiteRepository.deleteAllInBatch();
        return count;
    }

    // Appelé avant la suppression d'une zone : ses sites redeviennent non affectés.
    @Transactional
    public void detachZone(UUID zoneId) {
        zoneSiteRepository.findByZone_Id(zoneId).forEach(site -> {
            site.setZone(null);
            site.setZoneManuelle(false);
        });
    }

    private ZoneSite get(UUID id) {
        return zoneSiteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Site introuvable"));
    }

    // Zone d'un site :
    //  1. zones de la même province dont la ville/territoire = territoire du site (à défaut, = sa
    //     ville) ;
    //  2. sinon, zones de la province dont une commune = territoire ou ville du site (cas de
    //     Kinshasa, où l'on renseigne souvent la commune) ;
    //  3. plusieurs zones possibles : on garde celle dont une commune figure dans l'adresse ou la
    //     ville du site. Toujours ambigu, ou aucune zone : le site reste non affecté.
    static Zone matchZone(ZoneSite site, List<Zone> zones) {
        String province = key(DrcProvinces.resolve(site.getProvince(), site.getVille()));
        if (province.isEmpty()) {
            return null;
        }
        List<Zone> inProvince = zones.stream().filter(z -> key(z.getProvince()).equals(province)).toList();
        String territoire = key(site.getTerritoire());
        String ville = key(site.getVille());
        String commune = key(site.getCommune());

        List<Zone> candidates = List.of();
        if (!territoire.isEmpty()) {
            candidates = inProvince.stream().filter(z -> key(z.getVille()).equals(territoire)).toList();
        }
        if (candidates.isEmpty() && !ville.isEmpty()) {
            candidates = inProvince.stream().filter(z -> key(z.getVille()).equals(ville)).toList();
        }
        if (candidates.isEmpty()) {
            candidates = inProvince.stream()
                    .filter(z -> z.getCommunes().stream().map(ZoneSiteService::key)
                            .anyMatch(c -> c.equals(territoire) || c.equals(ville) || (!commune.isEmpty() && c.equals(commune))))
                    .toList();
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        if (candidates.size() > 1 && !commune.isEmpty()) {
            // Commune renseignée : c'est elle qui désigne la zone.
            List<Zone> byCommune = candidates.stream()
                    .filter(z -> z.getCommunes().stream().map(ZoneSiteService::key).anyMatch(commune::equals))
                    .toList();
            if (byCommune.size() == 1) {
                return byCommune.get(0);
            }
        }
        if (candidates.size() > 1) {
            // On cherche une commune dans l'adresse et le territoire (s'il diffère de la ville).
            // Une commune homonyme de la ville (ex. commune KINSHASA dans la ville de Kinshasa)
            // ne départage qu'en dernier recours : le nom de la ville figure presque toujours
            // dans l'adresse sans désigner cette commune.
            String text = " " + key(site.getAdresse()) + " " + (territoire.equals(ville) ? "" : territoire) + " ";
            String villeKey = ville.isEmpty() ? territoire : ville;
            List<Zone> precise = withCommuneIn(candidates, text, c -> !c.equals(villeKey));
            if (precise.size() == 1) {
                return precise.get(0);
            }
            if (precise.isEmpty()) {
                List<Zone> any = withCommuneIn(candidates, text, c -> true);
                if (any.size() == 1) {
                    return any.get(0);
                }
            }
        }
        return null;
    }

    private static List<Zone> withCommuneIn(List<Zone> zones, String text, java.util.function.Predicate<String> keep) {
        return zones.stream()
                .filter(z -> z.getCommunes().stream().map(ZoneSiteService::key)
                        .filter(c -> !c.isEmpty() && keep.test(c))
                        .anyMatch(c -> text.matches(".*[^A-Z0-9]" + Pattern.quote(c) + "[^A-Z0-9].*")))
                .toList();
    }

    private static String identity(ZoneSite s) {
        return key(s.getNom()) + "|" + key(s.getProvince()) + "|" + key(s.getVille()) + "|" + key(s.getTerritoire());
    }

    // Majuscules, sans accents ni espaces superflus : "Kasaï" = "KASAI", "Mont-Ngafula " = "MONT-NGAFULA".
    static String key(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }

    private List<ZoneSite> parse(MultipartFile file) {
        List<ZoneSite> result = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int[] columns = {0, 1, 2, 3, 4, -1};
            boolean first = true;
            for (Row row : sheet) {
                if (first) {
                    first = false;
                    int[] fromHeader = headerColumns(row, formatter);
                    if (fromHeader != null) {
                        columns = fromHeader;
                        continue;
                    }
                }
                String nom = text(row, columns[0], formatter);
                if (nom.isEmpty()) {
                    continue;
                }
                String province = text(row, columns[2], formatter);
                String ville = upperOrNull(text(row, columns[3], formatter));
                result.add(new ZoneSite(
                        UUID.randomUUID(),
                        nom,
                        emptyToNull(text(row, columns[1], formatter)),
                        province.isEmpty() ? DrcProvinces.resolve(null, ville) : DrcProvinces.normalize(province),
                        ville,
                        upperOrNull(text(row, columns[4], formatter)),
                        upperOrNull(text(row, columns[5], formatter))));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le fichier Excel", e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Fichier Excel invalide ou illisible : " + e.getMessage());
        }
        return result;
    }

    // Repère les colonnes d'après la ligne d'en-tête ; null si la ligne n'est pas un en-tête (moins
    // de 2 intitulés reconnus). Une colonne absente de l'en-tête vaut -1 (champ laissé vide).
    private static int[] headerColumns(Row row, DataFormatter formatter) {
        int[] columns = {-1, -1, -1, -1, -1, -1};
        int found = 0;
        for (Cell cell : row) {
            String label = key(formatter.formatCellValue(cell)).toLowerCase();
            int index = -1;
            if (label.contains("adresse")) {
                index = 1;
            } else if (label.contains("province")) {
                index = 2;
            } else if (label.contains("territoire")) {
                index = 4;
            } else if (label.contains("commune")) {
                index = 5;
            } else if (label.contains("ville")) {
                index = 3;
            } else if (label.contains("nom") || label.contains("site")) {
                index = 0;
            }
            if (index >= 0 && columns[index] < 0) {
                columns[index] = cell.getColumnIndex();
                found++;
            }
        }
        if (found < 2) {
            return null;
        }
        if (columns[0] < 0) {
            throw new IllegalArgumentException("Colonne « Nom du site » introuvable dans l'en-tête du fichier");
        }
        return columns;
    }

    private static String text(Row row, int index, DataFormatter formatter) {
        if (index < 0) {
            return "";
        }
        Cell cell = row.getCell(index);
        return cell == null ? "" : formatter.formatCellValue(cell).trim().replaceAll("\\s+", " ");
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String upperOrNull(String value) {
        return value == null || value.isBlank() ? null : value.toUpperCase();
    }

    // Modèle de fichier à remplir, proposé en téléchargement sur la page Sites.
    public byte[] template() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sites");
            CellStyle bold = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);
            String[] headers = {"Nom du site", "Adresse", "Province", "Ville", "Territoire", "Commune"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(bold);
            }
            String[] example = {"École primaire Mapendo", "12, avenue de la Paix", "Kongo-Central", "Matadi", "Matadi", "Mvuzi"};
            Row row = sheet.createRow(1);
            for (int i = 0; i < example.length; i++) {
                row.createCell(i).setCellValue(example[i]);
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 28 * 256);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
