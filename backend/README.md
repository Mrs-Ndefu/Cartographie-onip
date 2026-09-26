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
