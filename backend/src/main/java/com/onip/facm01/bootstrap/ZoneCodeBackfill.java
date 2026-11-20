package com.onip.facm01.bootstrap;

import com.onip.facm01.zone.ZoneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Au démarrage : les zones sans code, ou avec un code d'un ancien format (A1...), reçoivent un
// code numéro de province + numéro dans la province (01-0001, 01-0002, 02-0001...), dans l'ordre
// de création. Sans effet ensuite.
@Component
public class ZoneCodeBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ZoneCodeBackfill.class);

    private final ZoneService zoneService;
    private final JdbcTemplate jdbc;

    public ZoneCodeBackfill(ZoneService zoneService, JdbcTemplate jdbc) {
        this.zoneService = zoneService;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        widenCodeColumn();
        int updated = zoneService.backfillCodes();
        if (updated > 0) {
            log.info("Code attribué à {} zone(s) existante(s)", updated);
        }
    }

    // La colonne zones.code a été créée en VARCHAR(4) pour l'ancien format (A1) ; ddl-auto=update
    // n'élargit pas une colonne existante. « SET DATA TYPE » est accepté par H2 et PostgreSQL.
    private void widenCodeColumn() {
        try {
            Integer length = jdbc.query(
                    "SELECT character_maximum_length FROM information_schema.columns "
                            + "WHERE LOWER(table_name) = 'zones' AND LOWER(column_name) = 'code'",
                    rs -> rs.next() ? rs.getInt(1) : null);
            if (length != null && length > 0 && length < 7) {
                jdbc.execute("ALTER TABLE zones ALTER COLUMN code SET DATA TYPE VARCHAR(7)");
                log.info("Colonne zones.code élargie à 7 caractères (format 01-0001)");
            }
        } catch (RuntimeException e) {
            log.warn("Élargissement de zones.code impossible : {}", e.getMessage());
        }
    }
}
