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

    // Liste des communes distinctes réellement présentes en base (ménages non archivés) —
    // alimente le filtre par commune du tableau de bord admin. Filtrable par ville (":ville" vide
    // = toutes les villes) pour que le menu commune ne propose que les communes de la ville
    // actuellement sélectionnée.
    @Query("""
            SELECT DISTINCT h.address.commune FROM Household h
            WHERE h.address.commune IS NOT NULL AND h.address.commune <> '' AND h.archived = false
              AND (:ville = '' OR h.address.ville = :ville)
            ORDER BY h.address.commune
            """)
    List<String> findDistinctCommunes(@Param("ville") String ville);

    // Idem pour la ville — alimente le filtre par ville du tableau de bord admin. Filtrable par
    // commune, symétriquement à findDistinctCommunes(ville) : les deux menus se restreignent
    // mutuellement.
    @Query("""
            SELECT DISTINCT h.address.ville FROM Household h
            WHERE h.address.ville IS NOT NULL AND h.address.ville <> '' AND h.archived = false
              AND (:commune = '' OR h.address.commune = :commune)
            ORDER BY h.address.ville
            """)
    List<String> findDistinctVilles(@Param("commune") String commune);

    // Recherche unifiée du tableau de bord admin : nom du chef (nom/postnom/prénom) OU code
    // ménage, filtres ville/commune, plage de dates de création, et bascule archivés/actifs. Les
    // paramètres vides/null désactivent le filtre correspondant. Le service (HouseholdService)
    // remplace null par des valeurs neutres ("" / bornes de dates larges) avant l'appel : des
    // conditions "OR :x IS NULL" ici feraient échouer PostgreSQL ("could not determine data
    // type of parameter"), le driver ne pouvant pas déduire le type d'un paramètre qui n'apparaît
    // que dans un test IS NULL.
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
              AND (:ville = '' OR h.address.ville = :ville)
              AND (:commune = '' OR h.address.commune = :commune)
              AND h.createdAt >= :dateFrom
              AND h.createdAt <= :dateTo
            """)
    Page<Household> searchAdmin(
            @Param("search") String search,
            @Param("ville") String ville,
            @Param("commune") String commune,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("archived") boolean archived,
            Pageable pageable);
}
