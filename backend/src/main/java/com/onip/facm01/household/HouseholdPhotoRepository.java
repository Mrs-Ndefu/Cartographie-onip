package com.onip.facm01.household;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HouseholdPhotoRepository extends JpaRepository<HouseholdPhoto, UUID> {

    List<HouseholdPhoto> findByHousehold_IdOrderByPosition(UUID householdId);

    void deleteByHousehold_Id(UUID householdId);
}
