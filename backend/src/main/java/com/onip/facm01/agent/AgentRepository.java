package com.onip.facm01.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<Agent, UUID> {
    Optional<Agent> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Agent> findByZone_Id(UUID zoneId);

    long countByZone_Id(UUID zoneId);

    Page<Agent> findBySuperviseur_Id(UUID superviseurId, Pageable pageable);

    List<Agent> findByRoleAndActiveTrueOrderByFullNameAsc(AgentRole role);

    List<Agent> findBySuperviseur_Id(UUID superviseurId);

    boolean existsBySuperviseur_Id(UUID superviseurId);

    // Recherche par nom ou adresse mail (zone de recherche au-dessus du tableau "Comptes", cf.
    // AgentAdminController#list, agents.html) — insensible à la casse, ":search" vide = pas de
    // filtre (même convention que HouseholdRepository.searchAdmin).
    @Query("""
            SELECT a FROM Agent a
            WHERE :search = ''
               OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Agent> search(@Param("search") String search, Pageable pageable);

    @Query("""
            SELECT a FROM Agent a
            WHERE a.superviseur.id = :superviseurId
              AND (:search = ''
                   OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Agent> searchBySuperviseur(@Param("superviseurId") UUID superviseurId, @Param("search") String search, Pageable pageable);
}
