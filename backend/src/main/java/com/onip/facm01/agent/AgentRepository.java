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

    // Chefs d'équipe d'un superviseur donné — propose, pour l'affectation d'un agent, uniquement
    // SES chefs d'équipe (cf. AgentAdminController, menu "Encadrant" côté superviseur).
    List<Agent> findBySuperviseur_IdAndRoleAndActiveTrueOrderByFullNameAsc(UUID superviseurId, AgentRole role);

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

    // Équipe complète d'un superviseur : ses chefs d'équipe, les agents déjà affectés à chacun
    // d'eux (deux niveaux, cf. Agent.superviseur réutilisé pour CHEF_EQUIPE→SUPERVISEUR et
    // AGENT→CHEF_EQUIPE), et tout agent pas encore affecté à un chef d'équipe (candidats à
    // affecter — sans ça, le superviseur ne verrait jamais un agent tout juste créé pour pouvoir
    // le proposer à l'un de ses chefs d'équipe, cf. AgentAdminController#list et
    // #assignSupervisor). Le superviseur s'appuie sur la colonne Zone (agent comme chef d'équipe)
    // pour décider à qui l'affecter.
    // LEFT JOIN explicites (pas de simple "a.superviseur.id = ..." en chemin implicite) :
    // Hibernate traduirait sinon ce chemin en INNER JOIN, qui exclurait silencieusement les
    // agents sans superviseur AVANT même d'évaluer le OR "a.superviseur IS NULL" ci-dessous —
    // aucun agent non affecté n'apparaîtrait jamais, quel que soit le reste de la condition.
    @Query("""
            SELECT a FROM Agent a
            LEFT JOIN a.superviseur s
            LEFT JOIN s.superviseur s2
            WHERE (s.id = :superviseurId
                   OR s2.id = :superviseurId
                   OR (a.role = com.onip.facm01.agent.AgentRole.AGENT AND a.superviseur IS NULL))
              AND (:search = ''
                   OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Agent> searchByTeamOf(@Param("superviseurId") UUID superviseurId, @Param("search") String search, Pageable pageable);
}
