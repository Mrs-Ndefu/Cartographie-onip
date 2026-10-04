-- =============================================================================================
-- Adressage de Ménages (FACM01) — schéma de la base PostgreSQL
-- =============================================================================================
-- Généré à partir des entités JPA du backend (Hibernate, dialecte PostgreSQL) : il correspond
-- exactement au code au moment de la génération (commit qui ajoute ce fichier).
--
-- Utilisation (base vide) :
--   createdb -U facm01 carto_onip_rdc
--   psql -U facm01 -d carto_onip_rdc -f backend/database/schema-postgresql.sql
--
-- Ce script n'est pas obligatoire : au démarrage, le backend crée et met à jour lui-même les
-- tables (spring.jpa.hibernate.ddl-auto=update) et crée les comptes de départ (DataSeeder :
-- admin@onip.local / agent1@onip.local, mots de passe à changer). Il sert de référence pour
-- comprendre le modèle, ou pour préparer une base à la main.
--
-- Tables :
--   agents                  comptes (SUPER_ADMIN, ADMIN, SUPERVISEUR, DIRECTION_GENERALE, AGENT),
--                           zone d'affectation et superviseur de chaque agent
--   zones / zone_communes   zones d'affectation : 1 province + 1 ville + une ou plusieurs communes,
--                           avec un code : lettre de la province (A = Kinshasa) + numéro (A1, A2, B1...)
--   households              ménages (adresse, GPS, statut, retrait/archivage, agent d'origine)
--   household_members       chef (chef = true) et membres de chaque ménage
--   household_photos        photos de la fiche papier (jusqu'à 4 par ménage)
--   household_modifications historique des modifications faites depuis le tableau de bord (motif)
--
-- Pour régénérer après une évolution des entités : voir backend/README.md, « Schéma de données ».
-- =============================================================================================

create table agents (
        active boolean not null,
        created_at timestamp(6) with time zone not null,
        id uuid not null,
        superviseur_id uuid,
        zone_id uuid,
        full_name varchar(255) not null,
        password_hash varchar(255) not null,
        photo_content_type varchar(255),
        role varchar(255) not null check (role in ('SUPER_ADMIN','ADMIN','SUPERVISEUR','DIRECTION_GENERALE','AGENT')),
        username varchar(255) not null unique,
        photo oid,
        primary key (id)
    );

    create table household_members (
        chef boolean not null,
        position integer not null,
        household_id uuid not null,
        id uuid not null,
        date_naissance varchar(255),
        nom varchar(255),
        postnom varchar(255),
        prenom varchar(255),
        relation varchar(255),
        sexe varchar(255) check (sexe in ('M','F')),
        primary key (id)
    );

    create table household_modifications (
        modified_at timestamp(6) with time zone not null,
        household_id uuid not null,
        id uuid not null,
        author_role varchar(32) not null,
        motif varchar(1000) not null,
        author_full_name varchar(255),
        author_username varchar(255) not null,
        primary key (id)
    );

    create table household_photos (
        position integer not null,
        household_id uuid not null,
        id uuid not null,
        photo_content_type varchar(255) not null,
        photo oid not null,
        primary key (id)
    );

    create table households (
        archived boolean default false not null,
        gps_precision float(53),
        latitude float(53),
        longitude float(53),
        nombre_membres_declare integer,
        photo_count integer default 0 not null,
        saisie_manuelle boolean,
        archived_at timestamp(6) with time zone,
        created_at timestamp(6) with time zone not null,
        synced_at timestamp(6) with time zone,
        updated_at timestamp(6) with time zone not null,
        agent_id uuid,
        id uuid not null,
        code_menage varchar(18),
        agent_cartographe varchar(255),
        appartement varchar(255),
        commune varchar(255),
        date_encodage varchar(255),
        etage varchar(255),
        fait_a varchar(255),
        immeuble varchar(255),
        nombre_fiches varchar(255),
        numero varchar(255),
        province varchar(255),
        quartier varchar(255),
        renseignant varchar(255),
        rue varchar(255),
        status varchar(255) not null check (status in ('BROUILLON','COMPLET','A_VERIFIER')),
        ville varchar(255),
        primary key (id)
    );

    create table zone_communes (
        zone_id uuid not null,
        commune varchar(255) not null
    );

    create table zones (
        created_at timestamp(6) with time zone not null,
        id uuid not null,
        code varchar(4),
        province varchar(255) not null,
        ville varchar(255) not null,
        primary key (id)
    );

    alter table if exists agents 
       add constraint FKfn1e7u2pem5u20g69xqq6ur4c 
       foreign key (superviseur_id) 
       references agents;

    alter table if exists agents 
       add constraint FK5bdx4dd60qeqkfshu65dpc93 
       foreign key (zone_id) 
       references zones;

    alter table if exists household_members 
       add constraint FKits4dus4oxqsobbp02l23iw8x 
       foreign key (household_id) 
       references households;

    alter table if exists household_modifications 
       add constraint FKipl7167kqmuxk1ed6vq3dv07n 
       foreign key (household_id) 
       references households;

    alter table if exists household_photos 
       add constraint FKflm0tel874vug84wna93nn0jh 
       foreign key (household_id) 
       references households;

    alter table if exists households 
       add constraint FKh41ojh9qmyqr4muwct08j1kgp 
       foreign key (agent_id) 
       references agents;

    alter table if exists zone_communes 
       add constraint FKn4lonai9osrpv37jo3bl8yb16 
       foreign key (zone_id) 
       references zones;
