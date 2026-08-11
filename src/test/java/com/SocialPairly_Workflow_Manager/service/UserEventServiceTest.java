package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.EventParticipantProfileDto;
import com.SocialPairly_Workflow_Manager.dto.EventReactionResponseDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUploadResponseDto;
import com.SocialPairly_Workflow_Manager.dto.UserEventInvitationDto;
import com.SocialPairly_Workflow_Manager.dto.UserEventTileDto;
import com.SocialPairly_Workflow_Manager.dto.UserEventWorkspaceDto;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.EventParticipantRepository;
import com.SocialPairly_Workflow_Manager.repository.EventReactionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserIdentityBackgroundRepository;
import com.SocialPairly_Workflow_Manager.repository.UserLifeProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserPersonalityProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventServiceTest {

    @Mock
    private EventParticipantRepository eventParticipantRepository;

    @Mock
    private EventReactionRepository eventReactionRepository;

    @Mock
    private UserLifeProfileRepository userLifeProfileRepository;

    @Mock
    private UserPersonalityProfileRepository userPersonalityProfileRepository;

    @Mock
    private UserIdentityBackgroundRepository userIdentityBackgroundRepository;

    @Mock
    private UserMediaService userMediaService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserTokenService userTokenService;

    @InjectMocks
    private UserEventService userEventService;

    private User currentUser;
    private User otherUser;
    private EventDetail event;
    private EventParticipant selfParticipant;
    private EventParticipant otherParticipant;

    @BeforeEach
    void setUp() {
        currentUser = buildUser(1L, "Current", "User", null);
        otherUser = buildUser(2L, "Other", "Person", "Preferred");
        event = buildEvent();
        selfParticipant = buildParticipant(currentUser, RsvpStatus.accepted);
        otherParticipant = buildParticipant(otherUser, RsvpStatus.accepted);
    }

    @Test
    void listMyEventsShouldReturnTiles() {
        when(eventParticipantRepository.findPublishedEventsForUser(1L)).thenReturn(List.of(selfParticipant));
        when(eventParticipantRepository.countByEventId(10L)).thenReturn(3L);

        List<UserEventTileDto> tiles = userEventService.listMyEvents(currentUser);
        assertEquals(1, tiles.size());
        assertEquals(3, tiles.get(0).memberCount());
    }

    @Test
    void getInvitationShouldReturnPreviewData() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));

        UserEventInvitationDto invitation = userEventService.getInvitation(10L, currentUser);
        assertEquals(10L, invitation.eventId());
        assertEquals(2, invitation.memberCount());
        assertEquals(2, invitation.memberPreviews().size());
    }

    @Test
    void getInvitationShouldRejectUnpublishedEvent() {
        event.setStatus(EventStatus.draft);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));

        assertThrows(BadRequestException.class, () -> userEventService.getInvitation(10L, currentUser));
    }

    @Test
    void respondToInvitationShouldAcceptAndSendEmail() {
        selfParticipant.setRsvpStatus(RsvpStatus.pending);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.existsByEntryCode(anyString())).thenReturn(false);
        when(eventParticipantRepository.save(selfParticipant)).thenReturn(selfParticipant);
        when(eventParticipantRepository.countByEventId(10L)).thenReturn(2L);

        UserEventTileDto tile = userEventService.respondToInvitation(10L, currentUser, true);
        assertEquals(RsvpStatus.accepted, selfParticipant.getRsvpStatus());
        assertNotNull(selfParticipant.getEntryCode());
        verify(emailService).sendInvitationAcceptedEmail(eq(currentUser), eq(event), anyString());
        assertNotNull(tile.entryCode());
    }

    @Test
    void respondToInvitationShouldDecline() {
        selfParticipant.setRsvpStatus(RsvpStatus.pending);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.save(selfParticipant)).thenReturn(selfParticipant);
        when(eventParticipantRepository.countByEventId(10L)).thenReturn(2L);

        UserEventTileDto tile = userEventService.respondToInvitation(10L, currentUser, false);
        assertEquals(RsvpStatus.declined, selfParticipant.getRsvpStatus());
        verify(emailService, never()).sendInvitationAcceptedEmail(any(), any(), any());
        assertNotNull(tile);
    }

    @Test
    void respondToInvitationShouldRejectAlreadyResponded() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));

        assertThrows(BadRequestException.class,
                () -> userEventService.respondToInvitation(10L, currentUser, true));
    }

    @Test
    void getWorkspaceShouldReturnWorkspaceData() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(userTokenService.getBalance(currentUser)).thenReturn(50);

        UserEventWorkspaceDto workspace = userEventService.getWorkspace(10L, currentUser);
        assertEquals(50, workspace.tokenBalance());
        assertEquals(1, workspace.totalProfiles());
        assertEquals("NY-36061-1-1", workspace.participantCode());
    }

    @Test
    void getWorkspaceShouldRejectNonAcceptedParticipant() {
        selfParticipant.setRsvpStatus(RsvpStatus.pending);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));

        assertThrows(BadRequestException.class, () -> userEventService.getWorkspace(10L, currentUser));
    }

    @Test
    void listParticipantProfilesShouldExcludeSelf() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        stubProfileLookups(otherUser);

        List<EventParticipantProfileDto> profiles = userEventService.listParticipantProfiles(10L, currentUser);
        assertEquals(1, profiles.size());
        assertEquals("Preferred", profiles.get(0).displayName());
    }

    @Test
    void getParticipantProfileShouldReturnTargetProfile() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 2L))
                .thenReturn(Optional.of(otherParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        stubProfileLookups(otherUser);

        EventParticipantProfileDto profile = userEventService.getParticipantProfile(10L, currentUser, 2L);
        assertEquals(2L, profile.userId());
        assertEquals("Engineer", profile.occupation());
    }

    @Test
    void getParticipantProfileShouldRejectSelf() {
        assertThrows(BadRequestException.class,
                () -> userEventService.getParticipantProfile(10L, currentUser, 1L));
    }

    @Test
    void getParticipantProfileShouldRejectNonAcceptedTarget() {
        otherParticipant.setRsvpStatus(RsvpStatus.pending);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 2L))
                .thenReturn(Optional.of(otherParticipant));

        assertThrows(BadRequestException.class,
                () -> userEventService.getParticipantProfile(10L, currentUser, 2L));
    }

    @Test
    void getParticipantProfileShouldThrowWhenTargetNotFound() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userEventService.getParticipantProfile(10L, currentUser, 99L));
    }

    @Test
    void sendReactionShouldDeductTokensAndNotify() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 2L))
                .thenReturn(Optional.of(otherParticipant));
        when(eventReactionRepository.existsByEventIdAndFromUserIdAndToUserIdAndReactionType(
                10L, 1L, 2L, ReactionType.Heart)).thenReturn(false);
        when(userTokenService.deductTokens(currentUser, 3)).thenReturn(47);
        when(eventReactionRepository.save(any(EventReaction.class))).thenAnswer(inv -> inv.getArgument(0));

        EventReactionResponseDto response = userEventService.sendReaction(
                10L, currentUser, 2L, ReactionType.Heart);

        assertEquals(ReactionType.Heart, response.reactionType());
        assertEquals(3, response.tokensSpent());
        assertEquals(47, response.tokenBalance());
        verify(emailService).sendProfileReactionEmail(eq(otherUser), eq(currentUser), eq(event), eq(ReactionType.Heart));
    }

    @Test
    void sendReactionShouldRejectSelfReaction() {
        assertThrows(BadRequestException.class,
                () -> userEventService.sendReaction(10L, currentUser, 1L, ReactionType.Wave));
    }

    @Test
    void sendReactionShouldRejectDuplicateReaction() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 2L))
                .thenReturn(Optional.of(otherParticipant));
        when(eventReactionRepository.existsByEventIdAndFromUserIdAndToUserIdAndReactionType(
                10L, 1L, 2L, ReactionType.Wave)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> userEventService.sendReaction(10L, currentUser, 2L, ReactionType.Wave));
    }

    @Test
    void sendReactionShouldRejectInactiveTarget() {
        otherParticipant.setRsvpStatus(RsvpStatus.pending);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 2L))
                .thenReturn(Optional.of(otherParticipant));

        assertThrows(BadRequestException.class,
                () -> userEventService.sendReaction(10L, currentUser, 2L, ReactionType.Wave));
    }

    @Test
    void sendReactionShouldCoverAllReactionCosts() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 2L))
                .thenReturn(Optional.of(otherParticipant));
        when(eventReactionRepository.existsByEventIdAndFromUserIdAndToUserIdAndReactionType(
                anyLong(), anyLong(), anyLong(), any())).thenReturn(false);
        when(userTokenService.deductTokens(eq(currentUser), anyInt())).thenReturn(100);
        when(eventReactionRepository.save(any(EventReaction.class))).thenAnswer(inv -> inv.getArgument(0));

        for (ReactionType type : ReactionType.values()) {
            userEventService.sendReaction(10L, currentUser, 2L, type);
        }

        verify(userTokenService).deductTokens(currentUser, 1);
        verify(userTokenService).deductTokens(currentUser, 5);
    }

    @Test
    void memberPreviewShouldUseProfilePhotoAndInitial() {
        UserProfile profile = new UserProfile();
        profile.setProfilePhotoUrl("/photo.png");
        otherUser.setProfile(profile);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(otherParticipant));

        UserEventInvitationDto invitation = userEventService.getInvitation(10L, currentUser);
        assertEquals("/photo.png", invitation.memberPreviews().get(0).profilePhotoUrl());
        assertEquals("P", invitation.memberPreviews().get(0).initial());
    }

    @Test
    void buildProfileShouldUseFallbackPhotoWhenNoMedia() {
        UserProfile profile = new UserProfile();
        profile.setProfilePhotoUrl("/fallback.png");
        profile.setAboutMe("Hello");
        profile.setInterests(Set.of("art"));
        otherUser.setProfile(profile);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        when(userMediaService.getVisibleMediaForEventParticipant(2L, 1L)).thenReturn(List.of());
        stubMinimalProfileLookups(otherUser);

        List<EventParticipantProfileDto> profiles = userEventService.listParticipantProfiles(10L, currentUser);
        assertEquals("/fallback.png", profiles.get(0).mediaUrls().get(0));
    }

    @Test
    void getParticipantShouldThrowWhenNotAssigned() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userEventService.getInvitation(10L, currentUser));
    }

    @Test
    void respondToInvitationShouldFailWhenEntryCodeCannotBeGenerated() {
        selfParticipant.setRsvpStatus(RsvpStatus.pending);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.existsByEntryCode(anyString())).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> userEventService.respondToInvitation(10L, currentUser, true));
    }

    @Test
    void getWorkspaceShouldIncludeCheckedInParticipants() {
        otherParticipant.setRsvpStatus(RsvpStatus.checked_in);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(userTokenService.getBalance(currentUser)).thenReturn(25);

        UserEventWorkspaceDto workspace = userEventService.getWorkspace(10L, currentUser);
        assertEquals(1, workspace.totalProfiles());
    }

    @Test
    void listProfilesShouldIncludeSentReactionsForTarget() {
        EventReaction reaction = new EventReaction();
        reaction.setToUserId(2L);
        reaction.setReactionType(ReactionType.Wave);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of(reaction));
        stubProfileLookups(otherUser);

        List<EventParticipantProfileDto> profiles = userEventService.listParticipantProfiles(10L, currentUser);
        assertEquals("Wave", profiles.get(0).sentReactions().get(0));
    }

    @Test
    void memberPreviewShouldUseQuestionMarkForEmptyName() {
        User blankNameUser = buildUser(8L, "", "", null);
        EventParticipant blankParticipant = buildParticipant(blankNameUser, RsvpStatus.pending);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(blankParticipant));

        UserEventInvitationDto invitation = userEventService.getInvitation(10L, currentUser);
        assertEquals("?", invitation.memberPreviews().get(0).initial());
    }

    @Test
    void getParticipantProfileShouldUseTargetWhenNotInWithUserList() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 2L))
                .thenReturn(Optional.of(otherParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        stubProfileLookups(otherUser);

        EventParticipantProfileDto profile = userEventService.getParticipantProfile(10L, currentUser, 2L);
        assertEquals(2L, profile.userId());
    }

    @Test
    void buildProfileShouldHandleEducationWithBlankLevels() {
        UserProfile profile = new UserProfile();
        Education blankEducation = new Education();
        blankEducation.setEducationLevel("  ");
        Education validEducation = new Education();
        validEducation.setEducationLevel("MS");
        profile.setEducations(List.of(blankEducation, validEducation));
        otherUser.setProfile(profile);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        stubMinimalProfileLookups(otherUser);

        List<EventParticipantProfileDto> profiles = userEventService.listParticipantProfiles(10L, currentUser);
        assertEquals("MS", profiles.get(0).educationLevel());
    }

    @Test
    void buildLocationLabelShouldCombineCityAndState() {
        UserProfile profile = new UserProfile();
        profile.setLocationCity("Boston");
        otherUser.setProfile(profile);
        UserIdentityBackground identity = new UserIdentityBackground();
        identity.setStateRegion("MA");
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        when(userLifeProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(userPersonalityProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(userIdentityBackgroundRepository.findByUserId(2L)).thenReturn(Optional.of(identity));
        when(userMediaService.getVisibleMediaForEventParticipant(2L, 1L)).thenReturn(List.of());

        List<EventParticipantProfileDto> profiles = userEventService.listParticipantProfiles(10L, currentUser);
        assertEquals("Boston, MA", profiles.get(0).locationLabel());
    }

    @Test
    void getParticipantProfileShouldAllowCheckedInTarget() {
        otherParticipant.setRsvpStatus(RsvpStatus.checked_in);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 2L))
                .thenReturn(Optional.of(otherParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        stubProfileLookups(otherUser);

        assertEquals(2L, userEventService.getParticipantProfile(10L, currentUser, 2L).userId());
    }

    @Test
    void sendReactionShouldAllowCheckedInTarget() {
        otherParticipant.setRsvpStatus(RsvpStatus.checked_in);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 2L))
                .thenReturn(Optional.of(otherParticipant));
        when(eventReactionRepository.existsByEventIdAndFromUserIdAndToUserIdAndReactionType(
                10L, 1L, 2L, ReactionType.Spark)).thenReturn(false);
        when(userTokenService.deductTokens(currentUser, 2)).thenReturn(48);
        when(eventReactionRepository.save(any(EventReaction.class))).thenAnswer(inv -> inv.getArgument(0));

        EventReactionResponseDto response = userEventService.sendReaction(
                10L, currentUser, 2L, ReactionType.Spark);
        assertEquals(2, response.tokensSpent());
    }

    @Test
    void listProfilesShouldHandleNullProfile() {
        otherUser.setProfile(null);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        stubMinimalProfileLookups(otherUser);

        List<EventParticipantProfileDto> profiles = userEventService.listParticipantProfiles(10L, currentUser);
        assertTrue(profiles.get(0).interests().isEmpty());
        assertNull(profiles.get(0).occupation());
    }

    @Test
    void listProfilesShouldHandleNullEducationList() {
        UserProfile profile = new UserProfile();
        profile.setEducations(null);
        otherUser.setProfile(profile);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        stubMinimalProfileLookups(otherUser);

        assertNull(userEventService.listParticipantProfiles(10L, currentUser).get(0).educationLevel());
    }

    @Test
    void buildLocationLabelShouldIgnoreBlankStateRegion() {
        UserProfile profile = new UserProfile();
        profile.setLocationCity("Boston");
        otherUser.setProfile(profile);
        UserIdentityBackground identity = new UserIdentityBackground();
        identity.setStateRegion("   ");
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        when(userLifeProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(userPersonalityProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(userIdentityBackgroundRepository.findByUserId(2L)).thenReturn(Optional.of(identity));
        when(userMediaService.getVisibleMediaForEventParticipant(2L, 1L)).thenReturn(List.of());

        assertEquals("Boston", userEventService.listParticipantProfiles(10L, currentUser).get(0).locationLabel());
    }

    @Test
    void buildLocationLabelShouldUseStateOnlyWhenCityMissing() {
        otherUser.setProfile(new UserProfile());
        UserIdentityBackground identity = new UserIdentityBackground();
        identity.setStateRegion("Texas");
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant, otherParticipant));
        when(eventReactionRepository.findByEventIdAndFromUserId(10L, 1L)).thenReturn(List.of());
        when(userLifeProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(userPersonalityProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(userIdentityBackgroundRepository.findByUserId(2L)).thenReturn(Optional.of(identity));
        when(userMediaService.getVisibleMediaForEventParticipant(2L, 1L)).thenReturn(List.of());

        List<EventParticipantProfileDto> profiles = userEventService.listParticipantProfiles(10L, currentUser);
        assertEquals("Texas", profiles.get(0).locationLabel());
    }

    @Test
    void workspaceShouldAllowCheckedInSelfParticipant() {
        selfParticipant.setRsvpStatus(RsvpStatus.checked_in);
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdWithUser(10L))
                .thenReturn(List.of(selfParticipant));
        when(userTokenService.getBalance(currentUser)).thenReturn(10);

        assertNotNull(userEventService.getWorkspace(10L, currentUser));
    }

    @Test
    void sendReactionShouldThrowWhenTargetNotFound() {
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(selfParticipant));
        when(eventParticipantRepository.findByEventIdAndUserIdWithDetails(10L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userEventService.sendReaction(10L, currentUser, 2L, ReactionType.Wave));
    }

    private void stubProfileLookups(User user) {
        UserProfile profile = new UserProfile();
        profile.setDateOfBirth(LocalDate.now().minusYears(28));
        profile.setOccupation("Engineer");
        profile.setGender("F");
        profile.setReligion("None");
        profile.setLocationCity("Boston");
        profile.setAboutMe("About");
        profile.setInterests(Set.of("music"));
        Education education = new Education();
        education.setEducationLevel("BS");
        profile.setEducations(List.of(education));
        user.setProfile(profile);

        UserLifeProfile life = new UserLifeProfile();
        life.setMaritalStatus("Single");
        life.setHasChildren("No");
        UserPersonalityProfile personality = new UserPersonalityProfile();
        personality.setRelationshipGoals("Long-term");
        UserIdentityBackground identity = new UserIdentityBackground();
        identity.setStateRegion("MA");

        when(userLifeProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(life));
        when(userPersonalityProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(personality));
        when(userIdentityBackgroundRepository.findByUserId(user.getId())).thenReturn(Optional.of(identity));
        when(userMediaService.getVisibleMediaForEventParticipant(user.getId(), 1L))
                .thenReturn(List.of(MediaUploadResponseDto.builder().id(1L).mediaUrl("/media.png").build()));
    }

    private void stubMinimalProfileLookups(User user) {
        when(userLifeProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(userPersonalityProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(userIdentityBackgroundRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
    }

    private EventDetail buildEvent() {
        EventDetail e = new EventDetail();
        e.setId(10L);
        e.setEventCode("NY-36061-1");
        e.setTitle("Mixer");
        e.setVenueName("Venue");
        e.setLocation("NYC");
        e.setEventDate(LocalDate.of(2026, 6, 1));
        e.setEventTime(LocalTime.of(18, 0));
        e.setStatus(EventStatus.published);
        e.setEventPerks(List.of("Dinner"));
        return e;
    }

    private User buildUser(Long id, String first, String last, String preferred) {
        User user = new User();
        user.setId(id);
        user.setFirstName(first);
        user.setLastName(last);
        user.setPreferredName(preferred);
        user.setVerified(true);
        return user;
    }

    private EventParticipant buildParticipant(User user, RsvpStatus status) {
        EventParticipant participant = new EventParticipant();
        participant.setEvent(event);
        participant.setUser(user);
        participant.setRsvpStatus(status);
        participant.setEntryCode("ENTRY-" + user.getId());
        participant.setVerificationStatus(VerificationStatus.Pending);
        return participant;
    }
}
