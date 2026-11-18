package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.StaffRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRoleAssignmentRepository extends JpaRepository<StaffRoleAssignment, Long> {

    Optional<StaffRoleAssignment> findByEmailIgnoreCase(String email);

    Optional<StaffRoleAssignment> findByEmailIgnoreCaseAndActiveTrue(String email);

    List<StaffRoleAssignment> findAllByOrderByUpdatedAtDesc();

    List<StaffRoleAssignment> findByActiveTrueOrderByUpdatedAtDesc();

    boolean existsByEmailIgnoreCase(String email);
}
