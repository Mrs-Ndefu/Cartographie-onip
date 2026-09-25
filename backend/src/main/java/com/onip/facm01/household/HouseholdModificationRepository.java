package com.onip.facm01.household;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HouseholdModificationRepository extends JpaRepository<HouseholdModification, UUID> {
    List<HouseholdModification> findByHousehold_IdOrderByModifiedAtDesc(UUID householdId);
}
