# FACM01 — Backend (API de synchronisation + tableau de bord)

API Spring Boot + PostgreSQL qui reçoit les fiches ménage synchronisées depuis l'application
terrain (React, dossier racine), et expose un tableau de bord web pour les consulter.

## Démarrage

1. Lancer PostgreSQL en local :
   ```
   docker compose up -d
   ```
2. Lancer l'API (télécharge Maven automatiquement via le wrapper, pas besoin de Maven installé) :
   ```
   ./mvnw spring-boot:run
   ```
   Sous Windows : `mvnw.cmd spring-boot:run`

L'API démarre sur `http://localhost:8080`. Deux comptes de test sont créés automatiquement au
premier démarrage :

| Rôle | Usage | Utilisateur | Mot de passe |
|---|---|---|---|
| ADMIN | Tableau de bord (`/dashboard`) | `admin@onip.local` | `admin123` |
| AGENT | Connexion dans l'app terrain (React) pour tester la synchronisation | `agent1@onip.local` | `agent123` |

**Change ces mots de passe en production** (variables d'environnement `BOOTSTRAP_ADMIN_USERNAME`
/ `BOOTSTRAP_ADMIN_PASSWORD` / `BOOTSTRAP_AGENT_USERNAME` / `BOOTSTRAP_AGENT_PASSWORD`, voir
ci-dessous). Un compte ADMIN peut aussi se connecter dans l'app React (`/api/auth/login`
n'est pas restreint par rôle) — seul `/dashboard` et `/api/agents` exigent le rôle ADMIN.

## Tableau de bord

`http://localhost:8080/dashboard` (connexion avec le compte admin ci-dessus). Affiche le
nombre total de ménages, la répartition par statut, une carte des ménages géolocalisés et un
tableau détaillé, filtrable par province, ville, commune, zone, statut et dates. Dès qu'un
filtre de lieu est choisi, la carte zoome sur les ménages trouvés.

Droits par rôle :

| Rôle | Tableau de bord |
|---|---|
| SUPER_ADMIN | Voit tout (ménages, détails, retirés, zones), sans action sur les ménages. Gère tous les comptes. |
| ADMIN | Tout : modifier, retirer/restaurer un ménage, définir les zones, gérer les comptes (sauf ADMIN/SUPER_ADMIN). |
| SUPERVISEUR | Voit et modifie les ménages ; gère les comptes AGENT et les affecte à une zone. |
| DIRECTION_GENERALE | Uniquement `/dashboard/stats` : statistiques d'enregistrement et population totale. |
| AGENT | Pas d'accès au tableau de bord (apps de saisie uniquement). Affecté à une zone définie par l'ADMIN. |

Une zone = une province + une ville + une ou plusieurs communes de cette ville (pas de quartier).

## API

Toutes les routes `/api/**` sont protégées par JWT (sauf `/api/auth/login`).

- `POST /api/auth/login` — `{ "username", "password" }` → `{ "token", "expiresInMinutes", "agent" }`
- `POST /api/households/sync` — `{ "households": [...] }` (form JSON de l'app React) → upsert
  par `id`, dernière écriture gagne (comparaison sur `updatedAt`). Réservé aux utilisateurs
  authentifiés.
- `GET /api/households` — liste paginée
- `GET /api/households/{id}`
- `GET /api/agents`, `POST /api/agents` — gestion des agents, réservé au rôle `ADMIN`

## Variables d'environnement

| Variable | Défaut | Rôle |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | localhost / 5432 / carto_onip_rdc / facm01 / facm01 | Connexion PostgreSQL |
| `JWT_SECRET` | valeur de dev, **à changer** | Clé de signature des tokens (HS256, ≥32 caractères) |
| `JWT_EXPIRATION_MINUTES` | 10080 (7 jours) | Durée de validité du token — volontairement longue pour couvrir les périodes hors connexion sur le terrain |
| `BOOTSTRAP_ADMIN_USERNAME` / `BOOTSTRAP_ADMIN_PASSWORD` | admin@onip.local / admin123 | Compte admin créé au premier démarrage s'il n'existe pas déjà |
| `BOOTSTRAP_AGENT_USERNAME` / `BOOTSTRAP_AGENT_PASSWORD` | agent1@onip.local / agent123 | Compte agent de test créé au premier démarrage s'il n'existe pas déjà |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Origines autorisées (séparées par des virgules) pour les appels depuis l'app React |

## Schéma de données

Le schéma PostgreSQL complet (tables, clés, contraintes) est dans
[`database/schema-postgresql.sql`](database/schema-postgresql.sql).

Les données de la base de développement (comptes, zones, ménages, membres, photos) peuvent être
exportées en SQL PostgreSQL avec [`database/ExportData.java`](database/ExportData.java), qui produit
`database/data-postgresql.sql` et `database/data-photos-postgresql.sql`. Ces deux fichiers ne sont
pas versionnés (ils contiennent des données personnelles : comptes, ménages recensés, photos des
fiches) — ils se transmettent à part. À charger dans cet ordre, dans une base vide :

```
psql -U facm01 -d carto_onip_rdc -f database/schema-postgresql.sql
psql -U facm01 -d carto_onip_rdc -f database/data-postgresql.sql
psql -U facm01 -d carto_onip_rdc -f database/data-photos-postgresql.sql
```

Pour produire l'export (backend arrêté, H2 verrouillant son fichier), depuis la racine du dépôt :
`java -cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar backend/database/ExportData.java`. Pour le régénérer après une
évolution des entités (sans PostgreSQL, à partir du code) :

```
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:mem:schemagen --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=none --spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=database/schema-postgresql.sql --spring.jpa.properties.hibernate.hbm2ddl.delimiter=; --spring.jpa.properties.hibernate.format_sql=true --spring.main.web-application-type=none"
```

(Le démarrage s'arrête ensuite en erreur, les tables n'existant pas dans cette base temporaire :
c'est attendu, le fichier est déjà écrit. Remettre l'en-tête explicatif en tête du fichier.)

`spring.jpa.hibernate.ddl-auto=update` : le schéma PostgreSQL est créé/mis à jour
automatiquement au démarrage à partir des entités JPA. Pratique en développement — à
remplacer par des migrations versionnées (Flyway/Liquibase) avant une mise en production
sérieuse.

## Tests

```
./mvnw test
```
Le test `Facm01ApplicationTests` vérifie que le contexte Spring démarre (sécurité, JPA,
seed de l'admin) contre une base H2 en mémoire — pas besoin de PostgreSQL pour ce test.
