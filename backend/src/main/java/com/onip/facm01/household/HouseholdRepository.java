package com.onip.facm01.household;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {
    Page<Household> findAll(Pageable pageable);

    long countByStatus(HouseholdStatus status);

    long countByStatusAndArchivedFalse(HouseholdStatus status);

    long countByArchivedFalse();

    long countByArchivedTrue();

    Page<Household> findByAgent_Id(UUID agentId, Pageable pageable);

    @Query("SELECT h.createdAt FROM Household h WHERE h.archived = false")
    List<Instant> findAllCreatedAt();

    // Ménages enregistrés avant l'ajout de la province : complétés au démarrage (cf.
    // ProvinceBackfill) en déduisant la province de la ville.
    @Query("SELECT h FROM Household h WHERE h.address.province IS NULL AND h.address.ville IS NOT NULL")
    List<Household> findWithoutProvince();

    // Listes des provinces / villes / communes réellement présentes en base (ménages non retirés)
    // — alimentent les filtres du tableau de bord. Chaque liste se restreint selon les autres
    // filtres déjà choisis (":x" vide = pas de restriction), pour ne proposer que des choix qui
    // donnent des résultats.
    @Query("""
            SELECT DISTINCT h.address.province FROM Household h
            WHERE h.address.province IS NOT NULL AND h.address.province <> '' AND h.archived = false
            ORDER BY h.address.province
            """)
    List<String> findDistinctProvinces();

    @Query("""
            SELECT DISTINCT h.address.ville FROM Household h
            WHERE h.address.ville IS NOT NULL AND h.address.ville <> '' AND h.archived = false
              AND (:province = '' OR h.address.province = :province)
              AND (:commune = '' OR h.address.commune = :commune)
            ORDER BY h.address.ville
            """)
    List<String> findDistinctVilles(@Param("province") String province, @Param("commune") String commune);

    @Query("""
            SELECT DISTINCT h.address.commune FROM Household h
            WHERE h.address.commune IS NOT NULL AND h.address.commune <> '' AND h.archived = false
              AND (:province = '' OR h.address.province = :province)
              AND (:ville = '' OR h.address.ville = :ville)
            ORDER BY h.address.commune
            """)
    List<String> findDistinctCommunes(@Param("province") String province, @Param("ville") String ville);

    // Recherche unifiée du tableau de bord : nom du chef (nom/postnom/prénom) OU code ménage,
    // filtres province/ville/commune sans tenir compte de la casse (d'anciens ménages ont été
    // saisis en minuscules), communes d'une zone choisie (:anyZoneCommune = false quand aucune
    // zone : la liste reçoit alors une valeur factice, IN () étant invalide en SQL), statut
    // ("complet", "incomplet" = tout sauf complet), plage de dates de création, et bascule
    // retirés/actifs. Les paramètres vides désactivent le filtre correspondant. Le service
    // (HouseholdService) remplace null par des valeurs neutres ("" / bornes de dates larges)
    // avant l'appel : des conditions "OR :x IS NULL" ici feraient échouer PostgreSQL ("could not
    // determine data type of parameter"), le driver ne pouvant pas déduire le type d'un
    // paramètre qui n'apparaît que dans un test IS NULL.
    @Query("""
            SELECT DISTINCT h FROM Household h
            JOIN h.members m
            WHERE m.chef = true
              AND h.archived = :archived
              AND (:search = ''
                   OR LOWER(m.nom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(m.postnom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(m.prenom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(h.codeMenage) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:province = '' OR UPPER(h.address.province) = UPPER(:province))
              AND (:ville = '' OR UPPER(h.address.ville) = UPPER(:ville))
              AND (:commune = '' OR UPPER(h.address.commune) = UPPER(:commune))
              AND (:anyZoneCommune = false OR UPPER(h.address.commune) IN :zoneCommunes)
              AND (:statut = ''
                   OR (:statut = 'complet' AND h.status = com.onip.facm01.household.HouseholdStatus.COMPLET)
                   OR (:statut = 'incomplet' AND h.status <> com.onip.facm01.household.HouseholdStatus.COMPLET))
              AND h.createdAt >= :dateFrom
              AND h.createdAt <= :dateTo
            """)
    Page<Household> searchAdmin(
            @Param("search") String search,
            @Param("province") String province,
            @Param("ville") String ville,
            @Param("commune") String commune,
            @Param("anyZoneCommune") boolean anyZoneCommune,
            @Param("zoneCommunes") List<String> zoneCommunes,
            @Param("statut") String statut,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("archived") boolean archived,
            Pageable pageable);
}
