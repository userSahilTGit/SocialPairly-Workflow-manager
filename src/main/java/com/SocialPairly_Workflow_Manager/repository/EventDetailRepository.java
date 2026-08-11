package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.EventDetail;
import com.SocialPairly_Workflow_Manager.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventDetailRepository extends JpaRepository<EventDetail, Long> {

    Optional<EventDetail> findByEventCode(String eventCode);

    List<EventDetail> findByStatusOrderByEventDateDescEventTimeDesc(EventStatus status);

    List<EventDetail> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(e) FROM EventDetail e WHERE e.eventCode LIKE CONCAT(:prefix, '-%')")
    long countByEventCodePrefix(@Param("prefix") String prefix);

    long countByStatus(EventStatus status);
}
