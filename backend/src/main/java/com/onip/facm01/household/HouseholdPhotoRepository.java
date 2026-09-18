package com.onip.facm01.household;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HouseholdPhotoRepository extends JpaRepository<HouseholdPhoto, UUID> {
}
