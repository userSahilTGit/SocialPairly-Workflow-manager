package com.SocialPairly_Workflow_Manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_participants", uniqueConstraints = {
        @UniqueConstraint(name = "uq_event_user", columnNames = {"event_id", "user_id"})
})
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventDetail event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "rsvp_status", nullable = false, length = 20)
    private RsvpStatus rsvpStatus = RsvpStatus.pending;

    @Column(name = "entry_code", length = 50)
    private String entryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.Pending;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    public void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EventDetail getEvent() { return event; }
    public void setEvent(EventDetail event) { this.event = event; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public RsvpStatus getRsvpStatus() { return rsvpStatus; }
    public void setRsvpStatus(RsvpStatus rsvpStatus) { this.rsvpStatus = rsvpStatus; }

    public String getEntryCode() { return entryCode; }
    public void setEntryCode(String entryCode) { this.entryCode = entryCode; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
}
