package com.onip.facm01.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// Même problème que RoleColumnMigration, pour les colonnes à choix de la fiche de repérage : H2
// les crée en ENUM natif et PostgreSQL avec une contrainte CHECK, figées sur les valeurs des enums
// au moment de la création de la table. Les valeurs ajoutées pour le questionnaire terrain (BPEC,
// DOTATION, INUTILISABLE...) seraient refusées : on ramène ces colonnes à un simple texte.
@Component
@Order(0)
public class SiteEnumColumnMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SiteEnumColumnMigration.class);

    private static final List<String> COLUMNS = List.of(
            "type_site", "propriete_statut", "option_occupation", "type_infrastructure", "etat_batiment",
            "acces_routier", "stabilite_snel", "connectivite_internet", "avis_global");

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public SiteEnumColumnMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws SQLException {
        String product;
        try (Connection connection = dataSource.getConnection()) {
            product = connection.getMetaData().getDatabaseProductName();
        }
        for (String column : COLUMNS) {
            try {
                if ("H2".equalsIgnoreCase(product)) {
                    jdbcTemplate.execute("ALTER TABLE sites ALTER COLUMN " + column + " VARCHAR(40)");
                } else if ("PostgreSQL".equalsIgnoreCase(product)) {
                    jdbcTemplate.execute("ALTER TABLE sites DROP CONSTRAINT IF EXISTS sites_" + column + "_check");
                } else {
                    log.warn("Base {} : colonne sites.{} non migrée", product, column);
                }
            } catch (Exception e) {
                log.warn("Migration de sites.{} impossible : {}", column, e.getMessage());
            }
        }
    }
}
