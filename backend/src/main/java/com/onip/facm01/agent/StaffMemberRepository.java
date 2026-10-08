package com.onip.facm01.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffMemberRepository extends JpaRepository<StaffMember, java.util.UUID> {

    Optional<StaffMember> findByEmailIgnoreCase(String email);
}
