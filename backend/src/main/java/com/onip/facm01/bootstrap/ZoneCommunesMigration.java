package com.onip.facm01.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Une zone couvrait d'abord une seule commune (colonne zones.commune, + un quartier facultatif) ;
// elle couvre maintenant une ou plusieurs communes (table zone_communes) et n'a plus de quartier.
// ddl-auto=update crée la nouvelle table mais ne supprime jamais les anciennes colonnes :
// on recopie la commune de chaque zone existante, puis on retire commune et quartier (la colonne
// commune, NOT NULL et plus alimentée, ferait échouer la création de nouvelles zones).
@Component
@Order(0)
public class ZoneCommunesMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ZoneCommunesMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public ZoneCommunesMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (!columnExists("commune")) {
            return;
        }
        int copied = jdbcTemplate.update("""
                INSERT INTO zone_communes (zone_id, commune)
                SELECT z.id, z.commune FROM zones z
                WHERE z.commune IS NOT NULL
                  AND NOT EXISTS (SELECT 1 FROM zone_communes zc WHERE zc.zone_id = z.id AND zc.commune = z.commune)
                """);
        jdbcTemplate.execute("ALTER TABLE zones DROP COLUMN commune");
        if (columnExists("quartier")) {
            jdbcTemplate.execute("ALTER TABLE zones DROP COLUMN quartier");
        }
        log.info("Zones migrées vers plusieurs communes ({} commune(s) recopiée(s))", copied);
    }

    private boolean columnExists(String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE LOWER(table_name) = 'zones' AND LOWER(column_name) = ?
                """, Integer.class, column);
        return count != null && count > 0;
    }
}
