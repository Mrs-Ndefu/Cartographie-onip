package com.onip.facm01.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<Agent, UUID> {
    Optional<Agent> findByUsername(String username);

    boolean existsByUsername(String username);
}
