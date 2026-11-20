package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.SupportCallbackRequest;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SupportCallbackRequestRepository extends JpaRepository<SupportCallbackRequest, Long> {

    List<SupportCallbackRequest> findAllByOrderByCreatedAtDesc();

    List<SupportCallbackRequest> findByStatusOrderByCreatedAtDesc(SupportCallbackStatus status);

    long countByStatus(SupportCallbackStatus status);

    Optional<SupportCallbackRequest> findFirstByUser_IdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId,
            SupportCallbackStatus status,
            LocalDateTime createdAfter
    );
}
