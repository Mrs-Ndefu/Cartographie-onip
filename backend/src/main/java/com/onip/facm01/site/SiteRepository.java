package com.onip.facm01.site;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, UUID> {
    Page<Site> findByAgent_Id(UUID agentId, Pageable pageable);

    // Liste du tableau de bord (ADMIN/SUPER_ADMIN/SUPERVISEUR, toutes fiches) — recherche par nom
    // de site, ville ou commune, même convention que HouseholdRepository.searchAdmin.
    @Query("""
            SELECT s FROM Site s
            WHERE (:search = ''
                   OR LOWER(s.nomSite) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.ville) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.commune) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Site> searchAdmin(@Param("search") String search, Pageable pageable);

    // Empêche de supprimer définitivement un compte agent qui a déjà des fiches de repérage (même
    // principe que HouseholdRepository.existsByAgent_Id).
    boolean existsByAgent_Id(UUID agentId);
}
