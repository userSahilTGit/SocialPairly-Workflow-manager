package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event_details")
public class EventDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_code", nullable = false, unique = true, length = 50)
    private String eventCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    private String description;

    @Column(name = "venue_name", nullable = false, length = 255)
    private String venueName;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time", nullable = false)
    private LocalTime eventTime;

    @Column(name = "member_count")
    private Integer memberCount = 30;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_perks", columnDefinition = "json")
    private List<String> eventPerks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_type", nullable = false, length = 20)
    private EnrollmentType enrollmentType = EnrollmentType.SELECTED;

    @Column(name = "token_cost", nullable = false)
    private Integer tokenCost = 0;

    @Column(name = "male_slot_limit")
    private Integer maleSlotLimit;

    @Column(name = "female_slot_limit")
    private Integer femaleSlotLimit;

    @Column(name = "venue_latitude")
    private Double venueLatitude;

    @Column(name = "venue_longitude")
    private Double venueLongitude;

    @Column(name = "notify_radius_miles", nullable = false)
    private Integer notifyRadiusMiles = 100;

    @Column(name = "matching_opens_at")
    private LocalDateTime matchingOpensAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.draft;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EventParticipant> participants = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public LocalTime getEventTime() { return eventTime; }
    public void setEventTime(LocalTime eventTime) { this.eventTime = eventTime; }

    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }

    public List<String> getEventPerks() { return eventPerks == null ? new ArrayList<>() : eventPerks; }
    public void setEventPerks(List<String> eventPerks) { this.eventPerks = eventPerks == null ? new ArrayList<>() : eventPerks; }

    public EnrollmentType getEnrollmentType() { return enrollmentType; }
    public void setEnrollmentType(EnrollmentType enrollmentType) { this.enrollmentType = enrollmentType; }

    public Integer getTokenCost() { return tokenCost; }
    public void setTokenCost(Integer tokenCost) { this.tokenCost = tokenCost; }

    public Integer getMaleSlotLimit() { return maleSlotLimit; }
    public void setMaleSlotLimit(Integer maleSlotLimit) { this.maleSlotLimit = maleSlotLimit; }

    public Integer getFemaleSlotLimit() { return femaleSlotLimit; }
    public void setFemaleSlotLimit(Integer femaleSlotLimit) { this.femaleSlotLimit = femaleSlotLimit; }

    public Double getVenueLatitude() { return venueLatitude; }
    public void setVenueLatitude(Double venueLatitude) { this.venueLatitude = venueLatitude; }

    public Double getVenueLongitude() { return venueLongitude; }
    public void setVenueLongitude(Double venueLongitude) { this.venueLongitude = venueLongitude; }

    public Integer getNotifyRadiusMiles() { return notifyRadiusMiles; }
    public void setNotifyRadiusMiles(Integer notifyRadiusMiles) { this.notifyRadiusMiles = notifyRadiusMiles; }

    public LocalDateTime getMatchingOpensAt() { return matchingOpensAt; }
    public void setMatchingOpensAt(LocalDateTime matchingOpensAt) { this.matchingOpensAt = matchingOpensAt; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<EventParticipant> getParticipants() { return participants; }
    public void setParticipants(List<EventParticipant> participants) { this.participants = participants; }
}
