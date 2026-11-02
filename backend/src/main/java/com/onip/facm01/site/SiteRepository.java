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
              AND (:incompletes = false OR\s""" + PENDING_INCOMPLETE + ")")
    Page<Site> searchAdmin(
            @Param("search") String search,
            @Param("archived") boolean archived,
            @Param("incompletes") boolean incompletes,
            Pageable pageable);

    // Nombre affiché par la cloche de notification (ADMIN/SUPERVISEUR).
    @Query("SELECT COUNT(s) FROM Site s WHERE " + PENDING_INCOMPLETE)
    long countPendingIncomplete();

    // Carte du tableau de bord : seules les fiches non retirées avec un relevé GPS complet.
    List<Site> findByArchivedFalseAndLatitudeIsNotNullAndLongitudeIsNotNull();

    // Empêche de supprimer définitivement un compte agent qui a déjà des fiches de repérage (même
    // principe que HouseholdRepository.existsByAgent_Id).
    boolean existsByAgent_Id(UUID agentId);
}
