package com.SocialPairly_Workflow_Manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "user_personality_profile")
public class UserPersonalityProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "headline", length = 200)
    private String headline;

    @Lob
    @Column(name = "about_story")
    private String aboutStory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "friend_descriptors", columnDefinition = "json")
    private List<String> friendDescriptors = new ArrayList<>();

    @Column(name = "proud_of", length = 255)
    private String proudOf;

    @Column(name = "life_philosophy", length = 255)
    private String lifePhilosophy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personality_traits", columnDefinition = "json")
    private Set<String> personalityTraits = new HashSet<>();

    @Lob
    @Column(name = "interests_hobbies")
    private String interestsHobbies;

    @Lob
    @Column(name = "lifestyle_notes")
    private String lifestyleNotes;

    @Lob
    @Column(name = "relationship_goals")
    private String relationshipGoals;

    @Lob
    @Column(name = "first_date_prefs")
    private String firstDatePrefs;

    @Lob
    @Column(name = "ideal_partner")
    private String idealPartner;

    @Lob
    @Column(name = "extended_family")
    private String extendedFamily;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getAboutStory() { return aboutStory; }
    public void setAboutStory(String aboutStory) { this.aboutStory = aboutStory; }

    public List<String> getFriendDescriptors() {
        return friendDescriptors == null ? new ArrayList<>() : friendDescriptors;
    }
    public void setFriendDescriptors(List<String> friendDescriptors) {
        this.friendDescriptors = friendDescriptors == null ? new ArrayList<>() : friendDescriptors;
    }

    public String getProudOf() { return proudOf; }
    public void setProudOf(String proudOf) { this.proudOf = proudOf; }

    public String getLifePhilosophy() { return lifePhilosophy; }
    public void setLifePhilosophy(String lifePhilosophy) { this.lifePhilosophy = lifePhilosophy; }

    public Set<String> getPersonalityTraits() {
        return personalityTraits == null ? new HashSet<>() : personalityTraits;
    }
    public void setPersonalityTraits(Set<String> personalityTraits) {
        this.personalityTraits = personalityTraits == null ? new HashSet<>() : personalityTraits;
    }

    public String getInterestsHobbies() { return interestsHobbies; }
    public void setInterestsHobbies(String interestsHobbies) { this.interestsHobbies = interestsHobbies; }

    public String getLifestyleNotes() { return lifestyleNotes; }
    public void setLifestyleNotes(String lifestyleNotes) { this.lifestyleNotes = lifestyleNotes; }

    public String getRelationshipGoals() { return relationshipGoals; }
    public void setRelationshipGoals(String relationshipGoals) { this.relationshipGoals = relationshipGoals; }

    public String getFirstDatePrefs() { return firstDatePrefs; }
    public void setFirstDatePrefs(String firstDatePrefs) { this.firstDatePrefs = firstDatePrefs; }

    public String getIdealPartner() { return idealPartner; }
    public void setIdealPartner(String idealPartner) { this.idealPartner = idealPartner; }

    public String getExtendedFamily() { return extendedFamily; }
    public void setExtendedFamily(String extendedFamily) { this.extendedFamily = extendedFamily; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
