package com.onip.facm01.household;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {
    Page<Household> findAll(Pageable pageable);

    long countByStatus(HouseholdStatus status);

    @Query("SELECT h.createdAt FROM Household h")
    List<Instant> findAllCreatedAt();
}
