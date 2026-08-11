package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventEntityTest {

    @Test
    void eventDetailShouldSetTimestampsOnCreateAndUpdate() {
        EventDetail event = new EventDetail();
        assertNull(event.getCreatedAt());
        ReflectionTestUtils.invokeMethod(event, "onCreate");
        assertNotNull(event.getCreatedAt());
        assertNotNull(event.getUpdatedAt());

        LocalDateTime previous = event.getUpdatedAt();
        ReflectionTestUtils.invokeMethod(event, "onUpdate");
        assertNotNull(event.getUpdatedAt());
        assertTrue(!event.getUpdatedAt().isBefore(previous));
    }

    @Test
    void eventDetailShouldExposeAllFields() {
        EventDetail event = new EventDetail();
        event.setId(1L);
        event.setEventCode("TX-48113-1");
        event.setTitle("Event");
        event.setDescription("Desc");
        event.setVenueName("Venue");
        event.setLocation("Austin");
        event.setEventDate(LocalDate.of(2026, 3, 1));
        event.setEventTime(LocalTime.of(20, 0));
        event.setMemberCount(15);
        event.setEventPerks(new ArrayList<>(List.of("Drinks")));
        event.setStatus(EventStatus.published);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        event.setParticipants(new ArrayList<>());

        assertEquals("TX-48113-1", event.getEventCode());
        assertEquals(EventStatus.published, event.getStatus());
        assertEquals(1, event.getEventPerks().size());
        assertNotNull(event.getParticipants());
        assertEquals(15, event.getMemberCount());
    }

    @Test
    void eventDetailShouldHandleNullInternalPerksField() {
        EventDetail event = new EventDetail();
        ReflectionTestUtils.setField(event, "eventPerks", null);
        assertNotNull(event.getEventPerks());
        assertTrue(event.getEventPerks().isEmpty());
    }

    @Test
    void eventDetailShouldReplaceNullPerksListOnSetter() {
        EventDetail event = new EventDetail();
        event.setEventPerks(null);
        assertTrue(event.getEventPerks().isEmpty());
        event.setEventPerks(new ArrayList<>(List.of("Perk")));
        assertEquals(1, event.getEventPerks().size());
    }

    @Test
    void eventDetailShouldReturnEmptyPerksWhenNull() {
        EventDetail event = new EventDetail();
        event.setEventPerks(null);
        assertNotNull(event.getEventPerks());
        assertTrue(event.getEventPerks().isEmpty());
    }

    @Test
    void eventParticipantShouldSetAssignedAtOnCreate() {
        EventParticipant participant = new EventParticipant();
        assertNull(participant.getAssignedAt());
        ReflectionTestUtils.invokeMethod(participant, "onCreate");
        assertNotNull(participant.getAssignedAt());
    }

    @Test
    void eventParticipantShouldExposeAllFields() {
        EventDetail event = new EventDetail();
        User user = new User();
        user.setId(2L);
        EventParticipant participant = new EventParticipant();
        participant.setId(10L);
        participant.setEvent(event);
        participant.setUser(user);
        participant.setRsvpStatus(RsvpStatus.accepted);
        participant.setEntryCode("CODE");
        participant.setVerificationStatus(VerificationStatus.Done);
        participant.setAssignedAt(LocalDateTime.now());

        assertEquals(RsvpStatus.accepted, participant.getRsvpStatus());
        assertEquals("CODE", participant.getEntryCode());
        assertEquals(VerificationStatus.Done, participant.getVerificationStatus());
        assertSame(event, participant.getEvent());
        assertSame(user, participant.getUser());
    }

    @Test
    void eventReactionShouldSetCreatedAtOnCreate() {
        EventReaction reaction = new EventReaction();
        assertNull(reaction.getCreatedAt());
        ReflectionTestUtils.invokeMethod(reaction, "onCreate");
        assertNotNull(reaction.getCreatedAt());
    }

    @Test
    void eventReactionShouldExposeAllFields() {
        EventDetail event = new EventDetail();
        EventReaction reaction = new EventReaction();
        reaction.setId(5L);
        reaction.setEvent(event);
        reaction.setFromUserId(1L);
        reaction.setToUserId(2L);
        reaction.setReactionType(ReactionType.Spark);
        reaction.setTokensSpent(2);
        reaction.setCreatedAt(LocalDateTime.now());

        assertEquals(ReactionType.Spark, reaction.getReactionType());
        assertEquals(2, reaction.getTokensSpent());
        assertEquals(1L, reaction.getFromUserId());
        assertEquals(2L, reaction.getToUserId());
    }

    @Test
    void eventParticipantShouldNotOverwriteExistingAssignedAt() {
        EventParticipant participant = new EventParticipant();
        LocalDateTime existing = LocalDateTime.of(2020, 1, 1, 0, 0);
        participant.setAssignedAt(existing);
        ReflectionTestUtils.invokeMethod(participant, "onCreate");
        assertEquals(existing, participant.getAssignedAt());
    }

    @Test
    void eventReactionShouldNotOverwriteExistingCreatedAt() {
        EventReaction reaction = new EventReaction();
        LocalDateTime existing = LocalDateTime.of(2020, 1, 1, 0, 0);
        reaction.setCreatedAt(existing);
        ReflectionTestUtils.invokeMethod(reaction, "onCreate");
        assertEquals(existing, reaction.getCreatedAt());
    }

    @Test
    void eventDetailShouldReplaceNullPerksList() {
        EventDetail event = new EventDetail();
        event.setEventPerks(List.of("A"));
        event.setEventPerks(null);
        assertTrue(event.getEventPerks().isEmpty());
    }

    @Test
    void educationShouldExposeAllFields() {
        Education education = new Education();
        UserProfile profile = new UserProfile();
        education.setId(1L);
        education.setProfile(profile);
        education.setInstitution("MIT");
        education.setDegree("BS");
        education.setFieldOfStudy("CS");
        education.setStartYear(2010);
        education.setEndYear(2014);
        education.setEducationLevel("Bachelor");
        education.setCity("Boston");
        education.setCountryCode("US");
        education.setCurrentlyStudying(false);
        education.setHonors("Summa");
        education.setShowInstitutionPublicly(true);
        education.setVerificationDocumentId(99L);

        assertEquals("MIT", education.getInstitution());
        assertEquals("Bachelor", education.getEducationLevel());
        assertEquals("Boston", education.getCity());
        assertEquals("US", education.getCountryCode());
        assertFalse(education.getCurrentlyStudying());
        assertTrue(education.getShowInstitutionPublicly());
        assertEquals(99L, education.getVerificationDocumentId());
        assertSame(profile, education.getProfile());
    }
}
