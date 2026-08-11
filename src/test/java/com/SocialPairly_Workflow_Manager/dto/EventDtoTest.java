package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EventDtoTest {

    @Test
    void eventAdminStatsDtoShouldExposeValues() {
        EventAdminStatsDto dto = new EventAdminStatsDto(100, 10, 5, 25);
        assertEquals(100, dto.totalUsers());
        assertEquals(10, dto.publishedEvents());
        assertEquals(5, dto.draftEvents());
        assertEquals(25, dto.acceptedMembers());
    }

    @Test
    void eventSummaryDtoShouldMapFromEntity() {
        EventDetail event = buildEvent();
        EventSummaryDto dto = EventSummaryDto.from(event, 3, 2);
        assertEquals(event.getId(), dto.id());
        assertEquals(event.getEventCode(), dto.eventCode());
        assertEquals(3, dto.memberCount());
        assertEquals(2, dto.confirmedCount());
        assertEquals(EventStatus.draft, dto.status());
    }

    @Test
    void eventDetailDtoShouldMapFromEntity() {
        EventDetail event = buildEvent();
        EventParticipant participant = buildParticipant(event, buildUser(1L, "Ada", "Lovelace", null));
        EventParticipantDto participantDto = EventParticipantDto.from(participant, event.getEventCode());
        EventDetailDto dto = EventDetailDto.from(event, List.of(participantDto), 1);
        assertEquals(event.getTitle(), dto.title());
        assertEquals(1, dto.participants().size());
        assertEquals(1, dto.confirmedCount());
    }

    @Test
    void eventParticipantDtoShouldUsePreferredNameAndProfile() {
        EventDetail event = buildEvent();
        User user = buildUser(5L, "First", "Last", "Preferred");
        UserProfile profile = new UserProfile();
        profile.setGender("F");
        profile.setOccupation("Engineer");
        profile.setProfilePhotoUrl("/photo.png");
        user.setProfile(profile);
        EventParticipant participant = buildParticipant(event, user);
        participant.setRsvpStatus(RsvpStatus.accepted);
        participant.setEntryCode("CODE-123");
        participant.setVerificationStatus(VerificationStatus.Pending);

        EventParticipantDto dto = EventParticipantDto.from(participant, event.getEventCode());
        assertEquals("Preferred", dto.displayName());
        assertEquals("F", dto.gender());
        assertEquals("Engineer", dto.occupation());
        assertEquals("/photo.png", dto.profilePhotoUrl());
        assertEquals(event.getEventCode() + "-5", dto.participantCode());
        assertEquals(RsvpStatus.accepted, dto.rsvpStatus());
    }

    @Test
    void eventParticipantDtoShouldFallbackToFirstLastName() {
        EventDetail event = buildEvent();
        User user = buildUser(2L, "Grace", "Hopper", null);
        EventParticipantDto dto = EventParticipantDto.from(buildParticipant(event, user), event.getEventCode());
        assertEquals("Grace Hopper", dto.displayName());
    }

    @Test
    void eventCandidateDtoShouldMapUserProfiles() {
        User user = buildUser(10L, "Alan", "Turing", null);
        user.setVerified(true);
        UserProfile profile = new UserProfile();
        profile.setDateOfBirth(LocalDate.now().minusYears(30));
        profile.setGender("M");
        profile.setReligion("None");
        profile.setOccupation("Mathematician");
        profile.setLocationCity("London");
        profile.setProfilePhotoUrl("/alan.png");
        Education education = new Education();
        education.setEducationLevel("PhD");
        profile.setEducations(List.of(education));
        UserLifeProfile life = new UserLifeProfile();
        life.setMaritalStatus("Single");
        UserIdentityBackground identity = new UserIdentityBackground();
        identity.setStateRegion("England");

        EventCandidateDto dto = EventCandidateDto.from(user, profile, life, identity);
        assertEquals(10L, dto.userId());
        assertEquals("Alan Turing", dto.displayName());
        assertEquals(30, dto.age());
        assertEquals("London", dto.locationCity());
        assertEquals("England", dto.stateRegion());
        assertEquals("Single", dto.maritalStatus());
        assertEquals("PhD", dto.educationLevel());
        assertTrue(dto.verified());
    }

    @Test
    void eventCandidateDtoShouldHandleNullProfiles() {
        User user = buildUser(11L, "Jane", "Doe", "JD");
        EventCandidateDto dto = EventCandidateDto.from(user, null, null, null);
        assertEquals("JD", dto.displayName());
        assertNull(dto.age());
        assertNull(dto.gender());
        assertFalse(dto.verified());
    }

    @Test
    void userEventTileDtoShouldMapParticipant() {
        EventDetail event = buildEvent();
        User user = buildUser(3L, "Test", "User", null);
        EventParticipant participant = buildParticipant(event, user);
        participant.setEntryCode("ENTRY-1");

        UserEventTileDto dto = UserEventTileDto.from(participant, 5);
        assertEquals(event.getId(), dto.eventId());
        assertEquals(RsvpStatus.pending, dto.rsvpStatus());
        assertEquals(5, dto.memberCount());
        assertEquals(event.getEventCode() + "-3", dto.participantCode());
        assertEquals("ENTRY-1", dto.entryCode());
    }

    @Test
    void userEventInvitationDtoShouldMapEvent() {
        EventDetail event = buildEvent();
        List<EventMemberPreviewDto> previews = List.of(new EventMemberPreviewDto(1L, "/p.png", "A"));
        UserEventInvitationDto dto = UserEventInvitationDto.from(event, 2, previews);
        assertEquals(event.getId(), dto.eventId());
        assertEquals(2, dto.memberCount());
        assertEquals(1, dto.memberPreviews().size());
    }

    @Test
    void userEventWorkspaceDtoShouldMapEvent() {
        EventDetail event = buildEvent();
        UserEventWorkspaceDto dto = UserEventWorkspaceDto.from(event, "CODE-1", "ENTRY", 4, 100);
        assertEquals(100, dto.tokenBalance());
        assertEquals(4, dto.totalProfiles());
        assertEquals(0, dto.currentProfileIndex());
        assertEquals("ENTRY", dto.entryCode());
    }

    @Test
    void eventParticipantProfileDtoShouldExposeFields() {
        EventParticipantProfileDto dto = new EventParticipantProfileDto(
                1L, "E-1", "Name", 28, "City", "City, State", true,
                "F", "None", "Dev", "Single", "No", "BS",
                "About me", Set.of("music"), "Long-term", List.of("/m.png"), List.of("Wave")
        );
        assertEquals("Name", dto.displayName());
        assertEquals(1, dto.sentReactions().size());
        assertTrue(dto.interests().contains("music"));
    }

    @Test
    void requestAndResponseRecordsShouldExposeValues() {
        EventUpsertRequest upsert = new EventUpsertRequest(
                "Title", "Desc", "Venue", "Location",
                LocalDate.of(2026, 6, 1), LocalTime.of(18, 0),
                "NY", "36061", List.of("Dinner"), List.of(1L, 2L), true
        );
        assertEquals("Title", upsert.title());
        assertEquals(2, upsert.memberUserIds().size());
        assertTrue(upsert.notifyParticipants());

        EventRsvpRequest rsvp = new EventRsvpRequest(true);
        EventReactionRequest reaction = new EventReactionRequest(5L, ReactionType.Heart);
        EventReactionResponseDto response = new EventReactionResponseDto(ReactionType.Heart, 3, 47);
        VerifyEntryCodeRequest verify = new VerifyEntryCodeRequest("ABC123");

        assertTrue(rsvp.accept());
        assertEquals(ReactionType.Heart, reaction.reactionType());
        assertEquals(47, response.tokenBalance());
        assertEquals("ABC123", verify.entryCode());
    }

    @Test
    void eventMemberPreviewDtoShouldExposeValues() {
        EventMemberPreviewDto preview = new EventMemberPreviewDto(9L, "/photo.jpg", "Z");
        assertEquals(9L, preview.userId());
        assertEquals("Z", preview.initial());
    }

    @Test
    void eventParticipantDtoShouldHandleNullNames() {
        EventDetail event = buildEvent();
        User user = new User();
        user.setId(7L);
        user.setEmail("noname@example.com");
        EventParticipantDto dto = EventParticipantDto.from(buildParticipant(event, user), event.getEventCode());
        assertEquals("", dto.displayName());
    }

    @Test
    void eventCandidateDtoShouldSkipNullEducationLevels() {
        User user = buildUser(13L, "Pat", "Kim", null);
        UserProfile profile = new UserProfile();
        Education nullLevel = new Education();
        nullLevel.setEducationLevel(null);
        profile.setEducations(List.of(nullLevel));
        EventCandidateDto dto = EventCandidateDto.from(user, profile, null, null);
        assertTrue(dto.educationLevel() == null || dto.educationLevel().isBlank());
    }

    @Test
    void eventParticipantDtoShouldHandleBlankPreferredNameAndNullProfile() {
        EventDetail event = buildEvent();
        User user = buildUser(6L, "Only", "Name", "  ");
        EventParticipantDto dto = EventParticipantDto.from(buildParticipant(event, user), event.getEventCode());
        assertEquals("Only Name", dto.displayName());
        assertNull(dto.gender());
    }

    @Test
    void eventCandidateDtoShouldFilterBlankEducationLevels() {
        User user = buildUser(16L, "A", "B", null);
        UserProfile profile = new UserProfile();
        Education blank = new Education();
        blank.setEducationLevel("  ");
        Education valid = new Education();
        valid.setEducationLevel("BA");
        profile.setEducations(List.of(blank, valid));
        EventCandidateDto dto = EventCandidateDto.from(user, profile, null, null);
        assertEquals("BA", dto.educationLevel());
    }

    @Test
    void eventCandidateDtoShouldUsePreferredName() {
        User user = buildUser(14L, "Legal", "Name", "Nick");
        EventCandidateDto dto = EventCandidateDto.from(user, null, null, null);
        assertEquals("Nick", dto.displayName());
    }

    @Test
    void eventCandidateDtoShouldHandleNullLastName() {
        User user = new User();
        user.setId(15L);
        user.setFirstName("Single");
        user.setLastName(null);
        EventCandidateDto dto = EventCandidateDto.from(user, null, null, null);
        assertEquals("Single", dto.displayName());
    }

    @Test
    void eventCandidateDtoShouldJoinMultipleEducationLevels() {
        User user = buildUser(12L, "Sam", "Lee", null);
        UserProfile profile = new UserProfile();
        Education e1 = new Education();
        e1.setEducationLevel("BS");
        Education e2 = new Education();
        e2.setEducationLevel("MS");
        profile.setEducations(List.of(e1, e2));
        EventCandidateDto dto = EventCandidateDto.from(user, profile, null, null);
        assertEquals("BS, MS", dto.educationLevel());
    }

    private static EventDetail buildEvent() {
        EventDetail event = new EventDetail();
        event.setId(1L);
        event.setEventCode("NY-36061-1");
        event.setTitle("Summer Mixer");
        event.setDescription("Fun event");
        event.setVenueName("Grand Hall");
        event.setLocation("New York");
        event.setEventDate(LocalDate.of(2026, 7, 4));
        event.setEventTime(LocalTime.of(19, 0));
        event.setStatus(EventStatus.draft);
        event.setEventPerks(List.of("Dinner"));
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        return event;
    }

    private static User buildUser(Long id, String first, String last, String preferred) {
        User user = new User();
        user.setId(id);
        user.setFirstName(first);
        user.setLastName(last);
        user.setPreferredName(preferred);
        user.setEmail(first.toLowerCase() + "@example.com");
        return user;
    }

    private static EventParticipant buildParticipant(EventDetail event, User user) {
        EventParticipant participant = new EventParticipant();
        participant.setId(100L);
        participant.setEvent(event);
        participant.setUser(user);
        participant.setRsvpStatus(RsvpStatus.pending);
        participant.setVerificationStatus(VerificationStatus.Pending);
        participant.setAssignedAt(LocalDateTime.now());
        return participant;
    }
}
