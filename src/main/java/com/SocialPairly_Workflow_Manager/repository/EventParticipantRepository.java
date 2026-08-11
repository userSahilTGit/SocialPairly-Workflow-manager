package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.EventParticipant;
import com.SocialPairly_Workflow_Manager.entity.RsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    List<EventParticipant> findByEventIdOrderByAssignedAtAsc(Long eventId);

    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("""
            SELECT ep FROM EventParticipant ep
            JOIN FETCH ep.event
            JOIN FETCH ep.user
            WHERE ep.event.id = :eventId AND ep.user.id = :userId
            """)
    Optional<EventParticipant> findByEventIdAndUserIdWithDetails(
            @Param("eventId") Long eventId,
            @Param("userId") Long userId
    );

    @Query("SELECT ep FROM EventParticipant ep JOIN FETCH ep.user u LEFT JOIN FETCH u.profile WHERE ep.event.id = :eventId")
    List<EventParticipant> findByEventIdWithUser(@Param("eventId") Long eventId);

    long countByEventId(Long eventId);

    long countByEventIdAndRsvpStatus(Long eventId, RsvpStatus rsvpStatus);

    @Query("SELECT COUNT(ep) FROM EventParticipant ep WHERE ep.rsvpStatus = :status")
    long countByRsvpStatus(@Param("status") RsvpStatus status);

    @Query("""
            SELECT ep FROM EventParticipant ep
            JOIN FETCH ep.event e
            JOIN FETCH ep.user u
            LEFT JOIN FETCH u.profile
            WHERE u.id = :userId AND e.status = 'published'
            ORDER BY e.eventDate DESC, e.eventTime DESC
            """)
    List<EventParticipant> findPublishedEventsForUser(@Param("userId") Long userId);

    boolean existsByEntryCode(String entryCode);
}
