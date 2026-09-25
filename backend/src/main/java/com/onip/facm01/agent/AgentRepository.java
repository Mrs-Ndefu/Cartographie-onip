package com.onip.facm01.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
