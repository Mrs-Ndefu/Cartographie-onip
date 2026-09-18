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

    Page<Household> findByAgent_Id(UUID agentId, Pageable pageable);

    @Query("SELECT h.createdAt FROM Household h")
    List<Instant> findAllCreatedAt();

    // Recherche par nom du chef de ménage (nom, postnom ou prénom) — utilisée par le tableau
    // de bord admin (Thymeleaf) pour filtrer la liste des ménages sans recharger toute la page.
    @Query("""
            SELECT DISTINCT h FROM Household h
            JOIN h.members m
            WHERE m.chef = true
              AND (LOWER(m.nom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(m.postnom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(m.prenom) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Household> searchByChefName(@Param("search") String search, Pageable pageable);
}
