import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Exporte les données de la base de développement H2 (backend/data/carto_onip_rdc.mv.db) en
// instructions INSERT PostgreSQL, compatibles avec schema-postgresql.sql (à charger avant).
//
// Deux fichiers :
//   data-postgresql.sql         toutes les données, sans les photos (photos de profil à NULL)
//   data-photos-postgresql.sql  photos des fiches ménage et photos de profil (à charger après)
//
// Lancement (backend arrêté : H2 verrouille son fichier) :
//   java -cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar backend/database/ExportData.java
public class ExportData {

    private static final Path DB = Path.of("backend/data/carto_onip_rdc");
    private static final Path SCHEMA = Path.of("backend/database/schema-postgresql.sql");
    private static final Path OUT_DATA = Path.of("backend/database/data-postgresql.sql");
    private static final Path OUT_PHOTOS = Path.of("backend/database/data-photos-postgresql.sql");

    // Ordre d'insertion compatible avec les clés étrangères (agents.superviseur_id, qui pointe
    // vers agents, est renseigné à part, après l'insertion de tous les comptes).
    private static final List<String> TABLES = List.of(
            "zones", "zone_communes", "agents", "households", "household_members", "household_modifications");

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSxxx");

    public static void main(String[] args) throws Exception {
        Map<String, List<String>> schema = readSchema();
        String url = "jdbc:h2:file:" + DB.toAbsolutePath() + ";DATABASE_TO_LOWER=TRUE;ACCESS_MODE_DATA=r;IFEXISTS=TRUE";
        try (Connection c = DriverManager.getConnection(url, "sa", "sa")) {
            StringBuilder data = new StringBuilder(header(
                    "Données (sans photos) de la base de développement",
                    "à charger après schema-postgresql.sql"));
            data.append("BEGIN;\n\n");
            List<String> supervisorUpdates = new ArrayList<>();
            for (String table : TABLES) {
                List<String> columns = new ArrayList<>(schema.get(table));
                if (table.equals("agents")) {
                    columns.remove("photo");
                    columns.remove("photo_content_type");
                }
                int rows = 0;
                data.append("-- ").append(table).append('\n');
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT " + String.join(", ", columns) + " FROM " + table)) {
                    while (rs.next()) {
                        List<String> values = new ArrayList<>();
                        for (String col : columns) {
                            Object v = rs.getObject(col);
                            if (table.equals("agents") && col.equals("superviseur_id") && v != null) {
                                supervisorUpdates.add("UPDATE agents SET superviseur_id = " + literal(v)
                                        + " WHERE id = " + literal(rs.getObject("id")) + ";");
                                v = null;
                            }
                            values.add(literal(v));
                        }
                        data.append("INSERT INTO ").append(table).append(" (").append(String.join(", ", columns))
                                .append(") VALUES (").append(String.join(", ", values)).append(");\n");
                        rows++;
                    }
                }
                data.append("-- ").append(rows).append(" ligne(s)\n\n");
                System.out.println(table + " : " + rows);
                if (table.equals("agents") && !supervisorUpdates.isEmpty()) {
                    data.append("-- superviseur de chaque agent\n");
                    supervisorUpdates.forEach(u -> data.append(u).append('\n'));
                    data.append('\n');
                }
            }
            data.append("COMMIT;\n");
            Files.writeString(OUT_DATA, data.toString(), StandardCharsets.UTF_8);

            // Photos : grands objets PostgreSQL (colonnes oid), créés avec lo_from_bytea.
            StringBuilder photos = new StringBuilder(header(
                    "Photos de la base de développement (fiches ménage et profils)",
                    "à charger après data-postgresql.sql"));
            photos.append("BEGIN;\n\n");
            int householdPhotos = 0;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT id, household_id, position, photo_content_type, photo FROM household_photos")) {
                while (rs.next()) {
                    photos.append("INSERT INTO household_photos (id, household_id, position, photo_content_type, photo) VALUES (")
                            .append(literal(rs.getObject("id"))).append(", ")
                            .append(literal(rs.getObject("household_id"))).append(", ")
                            .append(rs.getInt("position")).append(", ")
                            .append(literal(rs.getString("photo_content_type"))).append(", ")
                            .append(largeObject(rs.getObject("photo"))).append(");\n");
                    householdPhotos++;
                }
            }
            int profilePhotos = 0;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT id, photo_content_type, photo FROM agents WHERE photo IS NOT NULL")) {
                while (rs.next()) {
                    photos.append("UPDATE agents SET photo_content_type = ").append(literal(rs.getString("photo_content_type")))
                            .append(", photo = ").append(largeObject(rs.getObject("photo")))
                            .append(" WHERE id = ").append(literal(rs.getObject("id"))).append(";\n");
                    profilePhotos++;
                }
            }
            photos.append("\nCOMMIT;\n");
            Files.writeString(OUT_PHOTOS, photos.toString(), StandardCharsets.UTF_8);
            System.out.println("household_photos : " + householdPhotos + ", photos de profil : " + profilePhotos);
        }
    }

    // Colonnes de chaque table, lues dans schema-postgresql.sql (seules celles-ci sont exportées :
    // la base H2 peut garder d'anciennes colonnes que le modèle actuel n'utilise plus).
    private static Map<String, List<String>> readSchema() throws Exception {
        String sql = Files.readString(SCHEMA, StandardCharsets.UTF_8);
        Map<String, List<String>> tables = new LinkedHashMap<>();
        Matcher m = Pattern.compile("create table (\\w+) \\((.*?)\\n\\s*\\);", Pattern.DOTALL).matcher(sql);
        while (m.find()) {
            List<String> cols = new ArrayList<>();
            for (String line : m.group(2).split("\\n")) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("primary key")) continue;
                cols.add(t.split("\\s+")[0]);
            }
            tables.put(m.group(1), cols);
        }
        return tables;
    }

    private static String literal(Object v) throws Exception {
        if (v == null) return "NULL";
        if (v instanceof Boolean b) return b ? "true" : "false";
        if (v instanceof Number n) return n.toString();
        if (v instanceof UUID u) return "'" + u + "'";
        if (v instanceof OffsetDateTime t) return "'" + TS.format(t) + "'";
        if (v instanceof java.sql.Timestamp t) return "'" + t + "'";
        return "'" + v.toString().replace("'", "''") + "'";
    }

    private static String largeObject(Object v) throws Exception {
        if (v == null) return "NULL";
        byte[] bytes = v instanceof Blob b ? b.getBytes(1, (int) b.length()) : (byte[]) v;
        return "lo_from_bytea(0, decode('" + HexFormat.of().formatHex(bytes) + "', 'hex'))";
    }

    private static String header(String title, String order) {
        return "-- " + "=".repeat(93) + "\n"
                + "-- Adressage de Ménages (FACM01) — " + title + "\n"
                + "-- " + "=".repeat(93) + "\n"
                + "-- Export de la base H2 de développement (backend/data), généré par ExportData.java.\n"
                + "-- PostgreSQL, " + order + " :\n"
                + "--   psql -U facm01 -d carto_onip_rdc -f backend/database/schema-postgresql.sql\n"
                + "--   psql -U facm01 -d carto_onip_rdc -f backend/database/data-postgresql.sql\n"
                + "--   psql -U facm01 -d carto_onip_rdc -f backend/database/data-photos-postgresql.sql\n"
                + "-- Données de test : comptes (mots de passe chiffrés BCrypt) et ménages saisis pendant les essais.\n"
                + "-- " + "=".repeat(93) + "\n\n";
    }
}
