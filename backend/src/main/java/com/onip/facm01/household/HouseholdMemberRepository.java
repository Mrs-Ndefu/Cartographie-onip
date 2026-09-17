package com.onip.facm01.household;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {

    @Query("SELECT m.sexe FROM HouseholdMember m WHERE m.sexe IS NOT NULL")
    List<Sexe> findAllSexe();
}
