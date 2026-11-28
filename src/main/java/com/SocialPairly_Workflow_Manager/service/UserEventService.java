package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.EventBookSlotResponseDto;
import com.SocialPairly_Workflow_Manager.dto.EventMemberPreviewDto;
import com.SocialPairly_Workflow_Manager.dto.EventParticipantProfileDto;
import com.SocialPairly_Workflow_Manager.dto.EventReactionResponseDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUploadResponseDto;
import com.SocialPairly_Workflow_Manager.dto.UserEventHomeStateDto;
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
import com.SocialPairly_Workflow_Manager.entity.UserEventNotification;
import com.SocialPairly_Workflow_Manager.entity.UserIdentityBackground;
import com.SocialPairly_Workflow_Manager.entity.UserLifeProfile;
import com.SocialPairly_Workflow_Manager.entity.UserPersonalityProfile;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.util.MediaUrls;
import com.SocialPairly_Workflow_Manager.entity.EnrollmentType;
import com.SocialPairly_Workflow_Manager.repository.EventDetailRepository;
import com.SocialPairly_Workflow_Manager.repository.EventParticipantRepository;
import com.SocialPairly_Workflow_Manager.repository.EventReactionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserEventNotificationRepository;
import com.SocialPairly_Workflow_Manager.repository.UserIdentityBackgroundRepository;
import com.SocialPairly_Workflow_Manager.repository.UserLifeProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserPersonalityProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class UserEventService {

    private static final Logger log = LoggerFactory.getLogger(UserEventService.class);

    private final EventParticipantRepository eventParticipantRepository;
    private final EventDetailRepository eventDetailRepository;
    private final EventReactionRepository eventReactionRepository;
    private final UserLifeProfileRepository userLifeProfileRepository;
    private final UserPersonalityProfileRepository userPersonalityProfileRepository;
    private final UserIdentityBackgroundRepository userIdentityBackgroundRepository;
    private final UserEventNotificationRepository userEventNotificationRepository;
    private final UserMediaService userMediaService;
    private final EmailService emailService;
    private final UserTokenService userTokenService;
    private final EventService eventService;

    public UserEventService(EventParticipantRepository eventParticipantRepository,
                            EventDetailRepository eventDetailRepository,
                            EventReactionRepository eventReactionRepository,
                            UserLifeProfileRepository userLifeProfileRepository,
                            UserPersonalityProfileRepository userPersonalityProfileRepository,
                            UserIdentityBackgroundRepository userIdentityBackgroundRepository,
                            UserEventNotificationRepository userEventNotificationRepository,
                            UserMediaService userMediaService,
                            EmailService emailService,
                            UserTokenService userTokenService,
                            EventService eventService) {
        this.eventParticipantRepository = eventParticipantRepository;
        this.eventDetailRepository = eventDetailRepository;
        this.eventReactionRepository = eventReactionRepository;
        this.userLifeProfileRepository = userLifeProfileRepository;
        this.userPersonalityProfileRepository = userPersonalityProfileRepository;
        this.userIdentityBackgroundRepository = userIdentityBackgroundRepository;
        this.userEventNotificationRepository = userEventNotificationRepository;
        this.userMediaService = userMediaService;
        this.emailService = emailService;
        this.userTokenService = userTokenService;
        this.eventService = eventService;
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

    @Transactional
    public UserEventHomeStateDto getHomeState(User user) {
        try {
            eventService.enrollUserIntoMatchingOpenEvents(user);
        } catch (Exception ex) {
            log.warn("Open-event enrollment on home-state failed for userId={}: {}", user.getId(), ex.getMessage());
        }

        int balance = userTokenService.getBalance(user);
        List<EventParticipant> mine = eventParticipantRepository.findPublishedEventsForUser(user.getId());
        EventParticipant featuredParticipant = pickFeaturedParticipant(mine);
        EventDetail openOpportunity = featuredParticipant == null ? findFeaturedOpenEvent() : null;

        EventDetail featuredEvent = featuredParticipant != null
                ? featuredParticipant.getEvent()
                : openOpportunity;
        RsvpStatus featuredStatus = featuredParticipant != null
                ? featuredParticipant.getRsvpStatus()
                : RsvpStatus.pending;

        int tokenCost = featuredEvent != null && featuredEvent.getTokenCost() != null
                ? featuredEvent.getTokenCost() : 0;

        boolean profileComplete = isProfileSectionsComplete(user);
        boolean phoneOk = user.isPhoneVerified();
        boolean emailOk = user.isEmailVerified();
        boolean tokensReady = balance >= Math.max(tokenCost, 0);
        // Phone verification is optional for unlock / booking; profile + tokens still required.
        boolean canBook = featuredEvent != null
                && featuredStatus == RsvpStatus.pending
                && profileComplete && tokensReady;

        UserEventHomeStateDto.UnlockStepsDto steps = new UserEventHomeStateDto.UnlockStepsDto(
                true, phoneOk, emailOk, profileComplete, tokensReady, canBook, balance, tokenCost);

        UserEventHomeStateDto.FeaturedEventDto featuredDto = featuredEvent != null
                ? toFeaturedDto(featuredEvent, featuredParticipant, user, balance) : null;
        UserEventHomeStateDto.VipPassDto vipPass = null;
        // New users always see the unlock roadmap until they have a booked seat.
        String phase = "UNLOCK";

        if (featuredParticipant != null) {
            EventDetail event = featuredParticipant.getEvent();
            LocalDateTime opensAt = matchingOpensAt(event);
            boolean accepted = featuredParticipant.getRsvpStatus() == RsvpStatus.accepted
                    || featuredParticipant.getRsvpStatus() == RsvpStatus.checked_in;
            if (accepted) {
                vipPass = toVipPass(featuredParticipant, user);
                if (LocalDateTime.now().isBefore(opensAt)) {
                    phase = "COUNTDOWN";
                } else {
                    phase = "LIVE";
                    dismissNotificationsForNonEnrolled(event.getId());
                }
            }
        }

        List<UserEventHomeStateDto.UserEventNotificationDto> notifications =
                userEventNotificationRepository.findByUserIdAndDismissedFalseOrderByCreatedAtDesc(user.getId())
                        .stream()
                        .map(n -> new UserEventHomeStateDto.UserEventNotificationDto(
                                n.getId(), n.getEventId(), n.getKind(), n.getTitle(), n.getBody(),
                                n.isSeen(), n.getCreatedAt()))
                        .toList();

        return new UserEventHomeStateDto(phase, steps, featuredDto, vipPass, notifications, balance);
    }

    @Transactional
    public EventBookSlotResponseDto bookSlot(Long eventId, User user) {
        EventDetail event = eventDetailRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        ensurePublished(event);

        EventParticipant participant = eventParticipantRepository.findByEventIdAndUserIdWithDetails(eventId, user.getId())
                .orElse(null);
        if (participant == null) {
            if (event.getEnrollmentType() != EnrollmentType.OPEN) {
                throw new ResourceNotFoundException("You are not assigned to this event");
            }
            participant = new EventParticipant();
            participant.setEvent(event);
            participant.setUser(user);
            participant.setRsvpStatus(RsvpStatus.pending);
            participant.setVerificationStatus(com.SocialPairly_Workflow_Manager.entity.VerificationStatus.Pending);
            participant.setInvitedVia("SELF");
            participant = eventParticipantRepository.save(participant);
        }

        if (participant.getRsvpStatus() == RsvpStatus.accepted
                || participant.getRsvpStatus() == RsvpStatus.checked_in) {
            throw new BadRequestException("You have already booked a slot for this event");
        }
        if (participant.getRsvpStatus() == RsvpStatus.declined) {
            throw new BadRequestException("You declined this invitation and cannot book a slot");
        }

        // Mobile phone verification is optional — do not block booking on phoneVerified.
        if (!isProfileSectionsComplete(user)) {
            throw new BadRequestException("Complete Identity, Personality, and Faith profile sections before booking");
        }

        int tokenCost = event.getTokenCost() != null ? event.getTokenCost() : 0;
        int balance = userTokenService.getBalance(user);
        if (balance < tokenCost) {
            throw new BadRequestException("You need " + tokenCost + " tokens to book this slot");
        }

        ensureGenderSlotAvailable(event, user);

        if (tokenCost > 0) {
            userTokenService.deductTokens(user, tokenCost);
        }

        participant.setRsvpStatus(RsvpStatus.accepted);
        participant.setEntryCode(generateUniqueEntryCode(event.getEventCode(), user.getId()));
        participant.setBookingTokensSpent(tokenCost);
        participant.setBookedAt(LocalDateTime.now());
        eventParticipantRepository.save(participant);

        userEventNotificationRepository.findByUserIdAndEventIdAndKind(user.getId(), eventId, "EVENT_INVITE")
                .ifPresent(n -> {
                    n.setSeen(true);
                    n.setDismissed(true);
                    userEventNotificationRepository.save(n);
                });

        emailService.sendInvitationAcceptedEmail(user, event, participant.getEntryCode());
        log.info("User {} booked slot for event {} tokens={}", user.getId(), eventId, tokenCost);

        return new EventBookSlotResponseDto(
                event.getId(),
                participant.getRsvpStatus().name(),
                participant.getEntryCode(),
                event.getEventCode() + "-" + user.getId(),
                tokenCost,
                userTokenService.getBalance(user),
                matchingOpensAt(event),
                event.getTitle(),
                event.getVenueName(),
                event.getLocation(),
                event.getEventDate(),
                event.getEventTime(),
                displayName(user)
        );
    }

    @Transactional
    public void dismissNotification(Long notificationId, User user) {
        UserEventNotification notification = userEventNotificationRepository
                .findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setDismissed(true);
        notification.setSeen(true);
        userEventNotificationRepository.save(notification);
    }

    @Transactional
    public void markNotificationSeen(Long notificationId, User user) {
        UserEventNotification notification = userEventNotificationRepository
                .findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setSeen(true);
        userEventNotificationRepository.save(notification);
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

        int tokenCost = event.getTokenCost() != null ? event.getTokenCost() : 0;
        if (accept && tokenCost > 0) {
            throw new BadRequestException("This event requires booking a slot with tokens. Use book-slot instead.");
        }

        if (accept) {
            ensureGenderSlotAvailable(event, user);
            participant.setRsvpStatus(RsvpStatus.accepted);
            participant.setEntryCode(generateUniqueEntryCode(event.getEventCode(), user.getId()));
            participant.setBookedAt(LocalDateTime.now());
            participant.setBookingTokensSpent(0);
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
        ensureMatchingOpen(self.getEvent());

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
        ensureMatchingOpen(self.getEvent());

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
        ensureMatchingOpen(self.getEvent());

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
        String fallbackPhoto = MediaUrls.usableProfilePhotoUrl(profile != null ? profile.getProfilePhotoUrl() : null);
        if (mediaUrls.isEmpty() && fallbackPhoto != null) {
            mediaUrls = List.of(fallbackPhoto);
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
        String photo = MediaUrls.usableProfilePhotoUrl(profile != null ? profile.getProfilePhotoUrl() : null);
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

    private void ensureMatchingOpen(EventDetail event) {
        LocalDateTime opensAt = matchingOpensAt(event);
        if (LocalDateTime.now().isBefore(opensAt)) {
            throw new BadRequestException("Matching opens when the event countdown finishes");
        }
    }

    private static LocalDateTime matchingOpensAt(EventDetail event) {
        if (event.getMatchingOpensAt() != null) {
            return event.getMatchingOpensAt();
        }
        return LocalDateTime.of(event.getEventDate(), event.getEventTime());
    }

    private boolean isProfileSectionsComplete(User user) {
        UserProfile profile = user.getProfile();
        boolean identityDone = profile != null && profile.getIdentityPage1CompletedAt() != null;
        boolean personalityDone = userPersonalityProfileRepository.findByUserId(user.getId()).isPresent();
        return identityDone && personalityDone;
    }

    private void ensureGenderSlotAvailable(EventDetail event, User user) {
        // Legacy / free events with no gender quotas skip this gate.
        if (event.getMaleSlotLimit() == null && event.getFemaleSlotLimit() == null) {
            return;
        }
        UserProfile profile = user.getProfile();
        String gender = profile != null ? profile.getGender() : null;
        if (gender == null || gender.isBlank()) {
            throw new BadRequestException("Set your gender in Identity & Background before booking a slot");
        }
        Integer limit = slotLimitForGender(event, gender);
        if (limit == null) {
            return;
        }
        long taken = countAcceptedByGender(event.getId(), gender);
        if (taken >= limit) {
            throw new BadRequestException("No slots remaining for your gender in this event");
        }
    }

    private Integer slotLimitForGender(EventDetail event, String gender) {
        String g = gender.trim().toLowerCase(Locale.US);
        if (g.startsWith("m") || g.contains("man") || g.equals("male")) {
            return event.getMaleSlotLimit();
        }
        if (g.startsWith("f") || g.contains("woman") || g.equals("female")) {
            return event.getFemaleSlotLimit();
        }
        return null;
    }

    private long countAcceptedByGender(Long eventId, String gender) {
        String normalized = gender.trim().toLowerCase(Locale.US);
        return eventParticipantRepository.findByEventIdWithUser(eventId).stream()
                .filter(p -> p.getRsvpStatus() == RsvpStatus.accepted || p.getRsvpStatus() == RsvpStatus.checked_in)
                .filter(p -> {
                    UserProfile profile = p.getUser().getProfile();
                    if (profile == null || profile.getGender() == null) {
                        return false;
                    }
                    return sameGenderBucket(normalized, profile.getGender());
                })
                .count();
    }

    private static boolean sameGenderBucket(String a, String b) {
        String left = a.toLowerCase(Locale.US);
        String right = b.trim().toLowerCase(Locale.US);
        boolean leftMale = left.startsWith("m") || left.contains("man") || left.equals("male");
        boolean rightMale = right.startsWith("m") || right.contains("man") || right.equals("male");
        boolean leftFemale = left.startsWith("f") || left.contains("woman") || left.equals("female");
        boolean rightFemale = right.startsWith("f") || right.contains("woman") || right.equals("female");
        if (leftMale && rightMale) return true;
        if (leftFemale && rightFemale) return true;
        return left.equals(right);
    }

    private EventParticipant pickFeaturedParticipant(List<EventParticipant> mine) {
        return mine.stream()
                .filter(p -> p.getRsvpStatus() == RsvpStatus.accepted || p.getRsvpStatus() == RsvpStatus.checked_in)
                .findFirst()
                .or(() -> mine.stream().filter(p -> p.getRsvpStatus() == RsvpStatus.pending).findFirst())
                .or(() -> mine.stream().findFirst())
                .orElse(null);
    }

    private EventDetail findFeaturedOpenEvent() {
        return eventDetailRepository.findByStatusOrderByEventDateDescEventTimeDesc(EventStatus.published).stream()
                .filter(e -> e.getEnrollmentType() == EnrollmentType.OPEN)
                .filter(e -> e.getEventDate() == null || !e.getEventDate().isBefore(LocalDate.now()))
                .findFirst()
                .orElseGet(() -> eventDetailRepository
                        .findByStatusOrderByEventDateDescEventTimeDesc(EventStatus.published).stream()
                        .filter(e -> e.getEventDate() == null || !e.getEventDate().isBefore(LocalDate.now()))
                        .findFirst()
                        .orElse(null));
    }

    private UserEventHomeStateDto.FeaturedEventDto toFeaturedDto(
            EventDetail event,
            EventParticipant participant,
            User user,
            int balance
    ) {
        UserProfile profile = user.getProfile();
        String gender = profile != null ? profile.getGender() : null;
        Integer limit = gender != null ? slotLimitForGender(event, gender) : null;
        Integer left = null;
        if (limit != null) {
            left = Math.max(0, limit - (int) countAcceptedByGender(event.getId(), gender));
        }
        int confirmed = (int) eventParticipantRepository.countByEventIdAndRsvpStatus(event.getId(), RsvpStatus.accepted);
        int members = event.getMemberCount() != null
                ? event.getMemberCount()
                : (int) eventParticipantRepository.countByEventId(event.getId());
        String rsvp = participant != null ? participant.getRsvpStatus().name() : RsvpStatus.pending.name();
        return new UserEventHomeStateDto.FeaturedEventDto(
                event.getId(),
                event.getEventCode(),
                event.getTitle(),
                event.getDescription(),
                event.getVenueName(),
                event.getLocation(),
                event.getEventDate(),
                event.getEventTime(),
                matchingOpensAt(event),
                rsvp,
                event.getTokenCost() != null ? event.getTokenCost() : 0,
                balance,
                limit,
                left,
                gender,
                event.getEventCode() + "-" + user.getId(),
                participant != null ? participant.getEntryCode() : null,
                confirmed,
                members
        );
    }

    private UserEventHomeStateDto.VipPassDto toVipPass(EventParticipant participant, User user) {
        EventDetail event = participant.getEvent();
        UserProfile profile = user.getProfile();
        String photo = MediaUrls.usableProfilePhotoUrl(profile != null ? profile.getProfilePhotoUrl() : null);
        return new UserEventHomeStateDto.VipPassDto(
                participant.getEntryCode(),
                event.getEventCode() + "-" + user.getId(),
                displayName(user),
                photo,
                event.getTitle(),
                event.getVenueName(),
                event.getLocation(),
                event.getEventDate(),
                event.getEventTime(),
                participant.getBookingTokensSpent(),
                "VIP Guest"
        );
    }

    private void dismissNotificationsForNonEnrolled(Long eventId) {
        List<Long> enrolledIds = getAcceptedParticipants(eventId).stream()
                .map(p -> p.getUser().getId())
                .toList();
        List<Long> toDismiss = userEventNotificationRepository.findByEventIdAndDismissedFalse(eventId).stream()
                .map(UserEventNotification::getUserId)
                .filter(id -> !enrolledIds.contains(id))
                .distinct()
                .toList();
        if (!toDismiss.isEmpty()) {
            userEventNotificationRepository.dismissForUsers(eventId, toDismiss);
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
