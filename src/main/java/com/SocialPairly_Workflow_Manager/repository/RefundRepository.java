package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.Refund;
import com.SocialPairly_Workflow_Manager.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    @Query("""
            SELECT r FROM Refund r
            JOIN FETCH r.payment
            JOIN FETCH r.user
            WHERE r.user.id = :userId
            ORDER BY r.createdAt DESC
            """)
    List<Refund> findAllByUserIdWithDetails(Long userId);

    @Query("""
            SELECT r FROM Refund r
            JOIN FETCH r.payment
            JOIN FETCH r.user
            ORDER BY r.createdAt DESC
            """)
    List<Refund> findAllWithDetails();

    @Query("""
            SELECT r FROM Refund r
            JOIN FETCH r.payment p
            LEFT JOIN FETCH p.subscription s
            LEFT JOIN FETCH s.plan
            JOIN FETCH r.user
            WHERE r.refundId = :refundId
            """)
    Optional<Refund> findByIdWithDetails(Long refundId);

    Optional<Refund> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);

    boolean existsByUser_IdAndStatusNotIn(Long userId, List<RefundStatus> terminalStatuses);

    void deleteByUser_Id(Long userId);
}
