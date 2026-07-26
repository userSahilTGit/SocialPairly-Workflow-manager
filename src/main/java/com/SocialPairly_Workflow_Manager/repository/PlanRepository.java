package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByIsActiveTrueOrderByDurationDaysAsc();

    Optional<Plan> findById(Long id);

    List<Plan> findAllByOrderByDurationDaysAsc();
}
