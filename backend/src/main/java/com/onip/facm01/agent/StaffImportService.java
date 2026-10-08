package com.onip.facm01.agent;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Import du fichier Excel (.xlsx) listant le personnel autorisé (colonne A : nom, colonne B :
// email, colonne C : rôle — optionnelle) — déposé par un ADMIN dans "Gérer les agents". Chaque
// import REMPLACE la liste précédente : le fichier est censé être la liste à jour, pas un ajout
// au fur et à mesure.
@Service
public class StaffImportService {

    private final StaffMemberRepository staffMemberRepository;

    public StaffImportService(StaffMemberRepository staffMemberRepository) {
        this.staffMemberRepository = staffMemberRepository;
    }

    @Transactional
    public int importFrom(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choisissez un fichier Excel (.xlsx) à importer");
        }
        List<StaffMember> parsed = parse(file);
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucune ligne valide trouvée (colonnes attendues : nom, email — première ligne ignorée si c'est un en-tête)");
        }
        // Remplace l'intégralité de la liste précédente par celle du fichier importé. Le flush
        // après deleteAll() est nécessaire : sans lui, Hibernate peut exécuter les INSERT avant
        // les DELETE dans la même transaction (il regroupe par type d'opération, pas par ordre
        // d'appel), ce qui viole la contrainte unique sur l'email si une même adresse réapparaît
        // d'un import à l'autre.
        staffMemberRepository.deleteAll();
        staffMemberRepository.flush();
        staffMemberRepository.saveAll(parsed);
        return parsed.size();
    }

    private List<StaffMember> parse(MultipartFile file) {
        List<StaffMember> result = new ArrayList<>();
        Instant now = Instant.now();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                // La première ligne est un en-tête ("Nom", "Email"...) si sa 2e colonne n'est pas
                // une adresse mail valide — on l'ignore dans ce cas, sinon on la traite comme une
                // ligne de données (fichier sans en-tête).
                String fullName = cellText(row, 0);
                String email = cellText(row, 1);
                String roleText = cellText(row, 2);
                if (fullName.isBlank() && email.isBlank()) {
                    continue;
                }
                if (row.getRowNum() == 0 && !looksLikeEmail(email)) {
                    continue;
                }
                if (!looksLikeEmail(email) || fullName.isBlank()) {
                    continue;
                }
                // Rôle non reconnu (colonne absente, vide, ou texte libre type intitulé de poste
                // qui ne correspond à aucun rôle de l'application) : on l'ignore plutôt que de
                // rejeter la ligne — c'est une aide à la saisie, pas une contrainte.
                AgentRole role = parseRole(roleText);
                result.add(new StaffMember(UUID.randomUUID(), email.trim().toLowerCase(), fullName.trim(), role, now));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le fichier Excel", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Fichier Excel invalide ou illisible : " + e.getMessage());
        }
        return result;
    }

    private static String cellText(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        String text = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> cell.toString();
        };
        return text.trim();
    }

    private static boolean looksLikeEmail(String value) {
        return value != null && value.contains("@") && value.contains(".");
    }

    // Accepte le nom exact du rôle (ex. "SUPERVISEUR", insensible à la casse) ou son libellé
    // affiché dans l'application (ex. "Superviseur", cf. AgentRole.label()).
    private static AgentRole parseRole(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        try {
            return AgentRole.valueOf(trimmed.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException ignored) {
            // Pas le nom exact de l'enum : on essaie le libellé affiché (ex. "Super admin").
        }
        for (AgentRole candidate : AgentRole.values()) {
            if (candidate.label().equalsIgnoreCase(trimmed)) {
                return candidate;
            }
        }
        return null;
    }
}
