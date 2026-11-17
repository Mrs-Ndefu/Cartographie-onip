package com.onip.facm01.site;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, UUID> {

    // Fiche active, sans décision de l'ADMIN/SUPERVISEUR et à laquelle il manque un champ
    // obligatoire — même règle que Site.champsManquants(), à garder alignées.
    String PENDING_INCOMPLETE = """
            (s.archived = false
             AND (s.validation IS NULL OR s.validation = com.onip.facm01.site.SiteValidation.EN_ATTENTE)
             AND (s.nomSite IS NULL OR TRIM(s.nomSite) = ''
                  OR s.province IS NULL OR TRIM(s.province) = ''
                  OR s.ville IS NULL OR TRIM(s.ville) = ''
                  OR s.commune IS NULL OR TRIM(s.commune) = ''
                  OR s.latitude IS NULL OR s.longitude IS NULL
                  OR s.typeSite IS NULL
                  OR (s.typeSite = com.onip.facm01.site.TypeSite.AUTRE
                      AND (s.typeSiteAutre IS NULL OR TRIM(s.typeSiteAutre) = ''))
                  OR s.proprieteStatut IS NULL
                  OR s.typeInfrastructure IS NULL
                  OR (s.typeInfrastructure = com.onip.facm01.site.TypeInfrastructure.AUTRE
                      AND (s.typeInfrastructureAutre IS NULL OR TRIM(s.typeInfrastructureAutre) = ''))))
            """;

    Page<Site> findByAgent_Id(UUID agentId, Pageable pageable);

    // Liste du tableau de bord (ADMIN/SUPER_ADMIN/SUPERVISEUR, toutes fiches) — recherche par nom
    // de site, ville ou commune, même convention que HouseholdRepository.searchAdmin.
    // incompletes=true : seulement les fiches signalées par la cloche (PENDING_INCOMPLETE).
    @Query("""
            SELECT s FROM Site s
            WHERE s.archived = :archived
              AND (:search = ''
                   OR LOWER(s.nomSite) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.ville) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.commune) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:province = '' OR UPPER(s.province) = UPPER(:province))
              AND (:ville = '' OR UPPER(s.ville) = UPPER(:ville))
              AND (:commune = '' OR UPPER(s.commune) = UPPER(:commune))
              AND (:quartier = '' OR UPPER(s.quartierGroupement) = UPPER(:quartier))
              AND (:incompletes = false OR\s""" + PENDING_INCOMPLETE + ")")
    Page<Site> searchAdmin(
            @Param("search") String search,
            @Param("province") String province,
            @Param("ville") String ville,
            @Param("commune") String commune,
            @Param("quartier") String quartier,
            @Param("archived") boolean archived,
            @Param("incompletes") boolean incompletes,
            Pageable pageable);

    // Valeurs présentes dans les fiches (retirées ou non, selon la liste affichée) pour les filtres
    // Province / Ville / Commune / Quartier du tableau Repérage, chacune restreinte par les niveaux
    // au-dessus déjà choisis ("" = pas de restriction).
    @Query("""
            SELECT DISTINCT UPPER(s.province) FROM Site s
            WHERE s.archived = :archived AND s.province IS NOT NULL AND s.province <> ''
            ORDER BY UPPER(s.province)
            """)
    List<String> distinctProvinces(@Param("archived") boolean archived);

    @Query("""
            SELECT DISTINCT UPPER(s.ville) FROM Site s
            WHERE s.archived = :archived AND s.ville IS NOT NULL AND s.ville <> ''
              AND (:province = '' OR UPPER(s.province) = UPPER(:province))
            ORDER BY UPPER(s.ville)
            """)
    List<String> distinctVilles(@Param("archived") boolean archived, @Param("province") String province);

    @Query("""
            SELECT DISTINCT UPPER(s.commune) FROM Site s
            WHERE s.archived = :archived AND s.commune IS NOT NULL AND s.commune <> ''
              AND (:province = '' OR UPPER(s.province) = UPPER(:province))
              AND (:ville = '' OR UPPER(s.ville) = UPPER(:ville))
            ORDER BY UPPER(s.commune)
            """)
    List<String> distinctCommunes(@Param("archived") boolean archived, @Param("province") String province,
                                  @Param("ville") String ville);

    @Query("""
            SELECT DISTINCT UPPER(s.quartierGroupement) FROM Site s
            WHERE s.archived = :archived AND s.quartierGroupement IS NOT NULL AND s.quartierGroupement <> ''
              AND (:province = '' OR UPPER(s.province) = UPPER(:province))
              AND (:ville = '' OR UPPER(s.ville) = UPPER(:ville))
              AND (:commune = '' OR UPPER(s.commune) = UPPER(:commune))
            ORDER BY UPPER(s.quartierGroupement)
            """)
    List<String> distinctQuartiers(@Param("archived") boolean archived, @Param("province") String province,
                                   @Param("ville") String ville, @Param("commune") String commune);

    // Nombre affiché par la cloche de notification (ADMIN/SUPERVISEUR).
    @Query("SELECT COUNT(s) FROM Site s WHERE " + PENDING_INCOMPLETE)
    long countPendingIncomplete();

    // Carte du tableau de bord : seules les fiches non retirées avec un relevé GPS complet.
    List<Site> findByArchivedFalseAndLatitudeIsNotNullAndLongitudeIsNotNull();

    // Fiches de repérage actives portant un nom de site : sert à savoir quels sites importés
    // (Zones > Sites) ont été visités, et si la fiche est complète (cf. ZoneSiteService.visits).
    List<Site> findByArchivedFalseAndNomSiteIsNotNull();

    // Empêche de supprimer définitivement un compte agent qui a déjà des fiches de repérage (même
    // principe que HouseholdRepository.existsByAgent_Id).
    boolean existsByAgent_Id(UUID agentId);
}
