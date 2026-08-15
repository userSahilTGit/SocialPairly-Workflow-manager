package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeRequest;
import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlanUpgradeRequestRepository extends JpaRepository<PlanUpgradeRequest, Long> {

    Optional<PlanUpgradeRequest> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);

    boolean existsByUser_IdAndStatusNotIn(Long userId, List<PlanUpgradeStatus> terminalStatuses);

    @Query("""
            SELECT r FROM PlanUpgradeRequest r
            JOIN FETCH r.user
            LEFT JOIN FETCH r.payment
            ORDER BY r.createdAt DESC
            """)
    List<PlanUpgradeRequest> findAllWithDetails();

    @Query("""
            SELECT r FROM PlanUpgradeRequest r
            JOIN FETCH r.user
            LEFT JOIN FETCH r.payment
            WHERE r.id = :id
            """)
    Optional<PlanUpgradeRequest> findByIdWithDetails(Long id);
}
