package com.onip.facm01.household;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {

    // Sexe et date de naissance de toutes les personnes (chefs et membres) des ménages non
    // retirés — base des statistiques démographiques de la Direction Générale.
    @Query("SELECT m.sexe, m.dateNaissance FROM HouseholdMember m WHERE m.household.archived = false")
    List<Object[]> findSexeAndDateNaissanceOfActiveHouseholds();
}
