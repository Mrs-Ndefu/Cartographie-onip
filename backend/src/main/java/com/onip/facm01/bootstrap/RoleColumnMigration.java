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

// Hibernate crée la colonne agents.role en ENUM natif sur H2 et avec une contrainte CHECK sur
// PostgreSQL, figées sur les valeurs de AgentRole au moment de la création de la table ;
// ddl-auto=update ne les met jamais à jour. Tout nouveau rôle (ex. DIRECTION_GENERALE) serait
// alors refusé à l'insertion. On ramène la colonne à un simple texte, avant tout autre
// traitement au démarrage (Order 0, le seeder crée des comptes juste après).
@Component
@Order(0)
public class RoleColumnMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleColumnMigration.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public RoleColumnMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws SQLException {
        String product;
        try (Connection connection = dataSource.getConnection()) {
            product = connection.getMetaData().getDatabaseProductName();
        }
        if ("H2".equalsIgnoreCase(product)) {
            jdbcTemplate.execute("ALTER TABLE agents ALTER COLUMN role VARCHAR(32) NOT NULL");
        } else if ("PostgreSQL".equalsIgnoreCase(product)) {
            jdbcTemplate.execute("ALTER TABLE agents DROP CONSTRAINT IF EXISTS agents_role_check");
        } else {
            log.warn("Base {} : colonne agents.role non migrée, un nouveau rôle pourrait être refusé", product);
        }
    }
}
