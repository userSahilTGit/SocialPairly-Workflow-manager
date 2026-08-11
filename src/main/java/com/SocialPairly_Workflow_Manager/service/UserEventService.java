package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.EventMemberPreviewDto;
import com.SocialPairly_Workflow_Manager.dto.EventParticipantProfileDto;
import com.SocialPairly_Workflow_Manager.dto.EventReactionResponseDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUploadResponseDto;
import com.SocialPairly_Workflow_Manager.dto.UserEventInvitationDto;
import com.SocialPairly_Workflow_Manager.dto.UserEventTileDto;
import com.SocialPairly_Workflow_Manager.dto.UserEventWorkspaceDto;
import com.SocialPairly_Workflow_Manager.entity.Education;
import com.SocialPairly_Workflow_Manager.entity.EventDetail;
import com.SocialPairly_Workflow_Manager.entity.EventParticipant;
import com.SocialPairly_Workflow_Manager.entity.EventReaction;
import com.SocialPairly_Workflow_Manager.entity.EventStatus;
import com.SocialPairly_Workflow_Manager.entity.ReactionType;
import com.SocialPairly_Workflow_Manager.entity.RsvpStatus;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserIdentityBackground;
import com.SocialPairly_Workflow_Manager.entity.UserLifeProfile;
import com.SocialPairly_Workflow_Manager.entity.UserPersonalityProfile;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.EventParticipantRepository;
import com.SocialPairly_Workflow_Manager.repository.EventReactionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserIdentityBackgroundRepository;
import com.SocialPairly_Workflow_Manager.repository.UserLifeProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserPersonalityProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class UserEventService {

    private static final Logger log = LoggerFactory.getLogger(UserEventService.class);

    private final EventParticipantRepository eventParticipantRepository;
    private final EventReactionRepository eventReactionRepository;
    private final UserLifeProfileRepository userLifeProfileRepository;
    private final UserPersonalityProfileRepository userPersonalityProfileRepository;
    private final UserIdentityBackgroundRepository userIdentityBackgroundRepository;
    private final UserMediaService userMediaService;
    private final EmailService emailService;
    private final UserTokenService userTokenService;

    public UserEventService(EventParticipantRepository eventParticipantRepository,
                            EventReactionRepository eventReactionRepository,
                            UserLifeProfileRepository userLifeProfileRepository,
                            UserPersonalityProfileRepository userPersonalityProfileRepository,
                            UserIdentityBackgroundRepository userIdentityBackgroundRepository,
                            UserMediaService userMediaService,
                            EmailService emailService,
                            UserTokenService userTokenService) {
        this.eventParticipantRepository = eventParticipantRepository;
        this.eventReactionRepository = eventReactionRepository;
        this.userLifeProfileRepository = userLifeProfileRepository;
        this.userPersonalityProfileRepository = userPersonalityProfileRepository;
        this.userIdentityBackgroundRepository = userIdentityBackgroundRepository;
        this.userMediaService = userMediaService;
        this.emailService = emailService;
        this.userTokenService = userTokenService;
    }

    @Transactional(readOnly = true)
    public List<UserEventTileDto> listMyEvents(User user) {
        return eventParticipantRepository.findPublishedEventsForUser(user.getId()).stream()
                .map(participant -> {
                    int memberCount = (int) eventParticipantRepository.countByEventId(participant.getEvent().getId());
                    return UserEventTileDto.from(participant, memberCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public UserEventInvitationDto getInvitation(Long eventId, User user) {
        EventParticipant participant = getParticipantOrThrow(eventId, user.getId());
        EventDetail event = participant.getEvent();
        ensurePublished(event);

        List<EventParticipant> members = eventParticipantRepository.findByEventIdWithUser(eventId);
        List<EventMemberPreviewDto> previews = members.stream()
                .map(this::toMemberPreview)
                .toList();

        return UserEventInvitationDto.from(event, members.size(), previews);
    }

    @Transactional
    public UserEventTileDto respondToInvitation(Long eventId, User user, boolean accept) {
        EventParticipant participant = getParticipantOrThrow(eventId, user.getId());
        EventDetail event = participant.getEvent();
        ensurePublished(event);

        if (participant.getRsvpStatus() != RsvpStatus.pending) {
            throw new BadRequestException("Invitation has already been responded to");
        }

        if (accept) {
            participant.setRsvpStatus(RsvpStatus.accepted);
            participant.setEntryCode(generateUniqueEntryCode(event.getEventCode(), user.getId()));
            eventParticipantRepository.save(participant);
            emailService.sendInvitationAcceptedEmail(user, event, participant.getEntryCode());
            log.info("User {} accepted event {}", user.getId(), eventId);
        } else {
            participant.setRsvpStatus(RsvpStatus.declined);
            eventParticipantRepository.save(participant);
            log.info("User {} declined event {}", user.getId(), eventId);
        }

        int memberCount = (int) eventParticipantRepository.countByEventId(eventId);
        return UserEventTileDto.from(participant, memberCount);
    }

    @Transactional(readOnly = true)
    public UserEventWorkspaceDto getWorkspace(Long eventId, User user) {
        EventParticipant participant = getParticipantOrThrow(eventId, user.getId());
        ensureAccepted(participant);
        EventDetail event = participant.getEvent();
        ensurePublished(event);

        List<EventParticipant> accepted = getAcceptedParticipants(eventId);
        int others = (int) accepted.stream().filter(p -> !p.getUser().getId().equals(user.getId())).count();
        String participantCode = event.getEventCode() + "-" + user.getId();

        return UserEventWorkspaceDto.from(
                event, participantCode, participant.getEntryCode(), others, userTokenService.getBalance(user));
    }

    @Transactional(readOnly = true)
    public List<EventParticipantProfileDto> listParticipantProfiles(Long eventId, User user) {
        EventParticipant self = getParticipantOrThrow(eventId, user.getId());
        ensureAccepted(self);
        ensurePublished(self.getEvent());

        List<EventReaction> myReactions = eventReactionRepository.findByEventIdAndFromUserId(eventId, user.getId());

        return getAcceptedParticipants(eventId).stream()
                .filter(p -> !p.getUser().getId().equals(user.getId()))
                .map(p -> buildProfileDto(p, user, myReactions))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventParticipantProfileDto getParticipantProfile(Long eventId, User user, Long targetUserId) {
        if (user.getId().equals(targetUserId)) {
            throw new BadRequestException("Cannot view your own profile in the event browser");
        }
        EventParticipant self = getParticipantOrThrow(eventId, user.getId());
        ensureAccepted(self);
        ensurePublished(self.getEvent());

        EventParticipant target = eventParticipantRepository.findByEventIdAndUserIdWithDetails(eventId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found in this event"));
        if (target.getRsvpStatus() != RsvpStatus.accepted && target.getRsvpStatus() != RsvpStatus.checked_in) {
            throw new BadRequestException("This participant has not accepted the invitation yet");
        }

        List<EventParticipant> withUser = eventParticipantRepository.findByEventIdWithUser(eventId);
        EventParticipant loaded = withUser.stream()
                .filter(p -> p.getUser().getId().equals(targetUserId))
                .findFirst()
                .orElse(target);

        List<EventReaction> myReactions = eventReactionRepository.findByEventIdAndFromUserId(eventId, user.getId());
        return buildProfileDto(loaded, user, myReactions);
    }

    @Transactional
    public EventReactionResponseDto sendReaction(Long eventId, User user, Long toUserId, ReactionType reactionType) {
        if (user.getId().equals(toUserId)) {
            throw new BadRequestException("You cannot react to your own profile");
        }

        EventParticipant self = getParticipantOrThrow(eventId, user.getId());
        ensureAccepted(self);
        ensurePublished(self.getEvent());

        EventParticipant target = eventParticipantRepository.findByEventIdAndUserIdWithDetails(eventId, toUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found in this event"));
        if (target.getRsvpStatus() != RsvpStatus.accepted && target.getRsvpStatus() != RsvpStatus.checked_in) {
            throw new BadRequestException("This participant is not active in the event workspace");
        }

        if (eventReactionRepository.existsByEventIdAndFromUserIdAndToUserIdAndReactionType(
                eventId, user.getId(), toUserId, reactionType)) {
            throw new BadRequestException("You have already sent this reaction to this profile");
        }

        int tokensSpent = tokenCost(reactionType);
        int newBalance = userTokenService.deductTokens(user, tokensSpent);

        EventReaction reaction = new EventReaction();
        reaction.setEvent(self.getEvent());
        reaction.setFromUserId(user.getId());
        reaction.setToUserId(toUserId);
        reaction.setReactionType(reactionType);
        reaction.setTokensSpent(tokensSpent);
        eventReactionRepository.save(reaction);

        User targetUser = target.getUser();
        emailService.sendProfileReactionEmail(targetUser, user, self.getEvent(), reactionType);

        log.info("User {} sent {} reaction to user {} in event {}", user.getId(), reactionType, toUserId, eventId);
        return new EventReactionResponseDto(reactionType, tokensSpent, newBalance);
    }

    private EventParticipantProfileDto buildProfileDto(
            EventParticipant participant,
            User viewer,
            List<EventReaction> myReactions
    ) {
        User target = participant.getUser();
        UserProfile profile = target.getProfile();
        UserLifeProfile life = userLifeProfileRepository.findByUserId(target.getId()).orElse(null);
        UserPersonalityProfile personality = userPersonalityProfileRepository.findByUserId(target.getId()).orElse(null);
        UserIdentityBackground identity = userIdentityBackgroundRepository.findByUserId(target.getId()).orElse(null);

        Integer age = null;
        String occupation = null;
        String gender = null;
        String religion = null;
        String locationCity = null;
        String aboutMe = null;
        var interests = profile != null ? profile.getInterests() : java.util.Set.<String>of();
        String educationLevel = null;

        if (profile != null) {
            if (profile.getDateOfBirth() != null) {
                age = Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
            }
            occupation = profile.getOccupation();
            gender = profile.getGender();
            religion = profile.getReligion();
            locationCity = profile.getLocationCity();
            aboutMe = profile.getAboutMe();
            if (profile.getEducations() != null && !profile.getEducations().isEmpty()) {
                educationLevel = profile.getEducations().stream()
                        .map(Education::getEducationLevel)
                        .filter(level -> level != null && !level.isBlank())
                        .collect(Collectors.joining(", "));
            }
        }

        String locationLabel = buildLocationLabel(locationCity, identity);
        String relationshipGoals = personality != null ? personality.getRelationshipGoals() : null;
        String maritalStatus = life != null ? life.getMaritalStatus() : null;
        String hasChildren = life != null ? life.getHasChildren() : null;

        List<String> mediaUrls = userMediaService.getVisibleMediaForEventParticipant(target.getId(), viewer.getId())
                .stream()
                .map(MediaUploadResponseDto::getMediaUrl)
                .toList();
        if (mediaUrls.isEmpty() && profile != null && profile.getProfilePhotoUrl() != null) {
            mediaUrls = List.of(profile.getProfilePhotoUrl());
        }

        List<String> sentReactions = myReactions.stream()
                .filter(r -> r.getToUserId().equals(target.getId()))
                .map(r -> r.getReactionType().name())
                .toList();

        String participantCode = participant.getEvent().getEventCode() + "-" + target.getId();

        return new EventParticipantProfileDto(
                target.getId(),
                participantCode,
                displayName(target),
                age,
                locationCity,
                locationLabel,
                target.isVerified(),
                gender,
                religion,
                occupation,
                maritalStatus,
                hasChildren,
                educationLevel,
                aboutMe,
                interests,
                relationshipGoals,
                mediaUrls,
                sentReactions
        );
    }

    private EventMemberPreviewDto toMemberPreview(EventParticipant participant) {
        User user = participant.getUser();
        UserProfile profile = user.getProfile();
        String photo = profile != null ? profile.getProfilePhotoUrl() : null;
        String name = displayName(user);
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        return new EventMemberPreviewDto(user.getId(), photo, initial);
    }

    private String generateUniqueEntryCode(String eventCode, Long userId) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int random = ThreadLocalRandom.current().nextInt(1000, 10000);
            String code = eventCode + "-" + userId + "-" + random;
            if (!eventParticipantRepository.existsByEntryCode(code)) {
                return code;
            }
        }
        throw new BadRequestException("Unable to generate a unique entry code. Please try again.");
    }

    private List<EventParticipant> getAcceptedParticipants(Long eventId) {
        return eventParticipantRepository.findByEventIdWithUser(eventId).stream()
                .filter(p -> p.getRsvpStatus() == RsvpStatus.accepted || p.getRsvpStatus() == RsvpStatus.checked_in)
                .toList();
    }

    private EventParticipant getParticipantOrThrow(Long eventId, Long userId) {
        return eventParticipantRepository.findByEventIdAndUserIdWithDetails(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not assigned to this event"));
    }

    private void ensurePublished(EventDetail event) {
        if (event.getStatus() != EventStatus.published) {
            throw new BadRequestException("This event is not available");
        }
    }

    private void ensureAccepted(EventParticipant participant) {
        if (participant.getRsvpStatus() != RsvpStatus.accepted
                && participant.getRsvpStatus() != RsvpStatus.checked_in) {
            throw new BadRequestException("Accept the invitation to access this event workspace");
        }
    }

    private static int tokenCost(ReactionType type) {
        return switch (type) {
            case Wave -> 1;
            case Spark -> 2;
            case Heart -> 3;
            case Coffee -> 4;
            case Priority -> 5;
        };
    }

    private static String displayName(User user) {
        if (user.getPreferredName() != null && !user.getPreferredName().isBlank()) {
            return user.getPreferredName().trim();
        }
        return ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
    }

    private static String buildLocationLabel(String city, UserIdentityBackground identity) {
        List<String> parts = new ArrayList<>();
        if (city != null && !city.isBlank()) {
            parts.add(city.trim());
        }
        if (identity != null && identity.getStateRegion() != null && !identity.getStateRegion().isBlank()) {
            parts.add(identity.getStateRegion().trim());
        }
        return String.join(", ", parts);
    }
}
