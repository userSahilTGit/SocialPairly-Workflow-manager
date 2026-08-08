package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.MediaStatus;
import com.SocialPairly_Workflow_Manager.entity.MediaType;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserMedia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the privacy query layer (UserMediaRepository.findVisibleMediaForViewer).
 * Covers all branch combinations:
 *  - PUBLIC always visible when APPROVED
 *  - MUTUAL_ONLY visible only when isMutual=true
 *  - VERIFIED_ONLY visible only when isVerified=true
 *  - EVENT_ONLY visible only when isEventActive=true
 *  - non-APPROVED media never visible
 */
@DataJpaTest
@ActiveProfiles("test")
class UserMediaRepositoryIT {

    @Autowired
    private UserMediaRepository userMediaRepository;
    @Autowired
    private UserRepository userRepository;

    private User target;

    @BeforeEach
    void setUp() {
        target = new User();
        target.setFirstName("Target");
        target.setLastName("User");
        target.setEmail("target" + System.nanoTime() + "@example.com");
        target.setPhoneNumber(String.valueOf(System.nanoTime()));
        target.setPassword("pass");
        target.setVerified(false);
        userRepository.save(target);
    }

    private UserMedia media(MediaType type, String privacy, String category, boolean cover, MediaStatus status) {
        return UserMedia.builder()
                .user(target)
                .mediaData(new byte[]{1, 2, 3})
                .mediaType(type)
                .fileSizeKb(1.0)
                .status(status)
                .mediaCategory(category)
                .privacyMode(privacy)
                .isCover(cover)
                .requiresAccessApproval(false)
                .build();
    }

    private Long save(String privacy, MediaStatus status) {
        return userMediaRepository.save(media(MediaType.PHOTO, privacy, "FULL_LENGTH", false, status)).getId();
    }

    @Test
    @DisplayName("PUBLIC + APPROVED is always visible")
    void publicApprovedAlwaysVisible() {
        save("PUBLIC", MediaStatus.APPROVED);
        save("PUBLIC", MediaStatus.APPROVED);

        List<UserMedia> result = userMediaRepository.findVisibleMediaForViewer(target.getId(), false, false, false);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("PUBLIC but REJECTED/PENDING not visible")
    void publicNonApprovedHidden() {
        save("PUBLIC", MediaStatus.APPROVED);
        save("PUBLIC", MediaStatus.REJECTED);
        save("PUBLIC", MediaStatus.PENDING);

        List<UserMedia> result = userMediaRepository.findVisibleMediaForViewer(target.getId(), false, false, false);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("MUTUAL_ONLY visible only when mutual")
    void mutualOnlyOnlyWhenMutual() {
        save("MUTUAL_ONLY", MediaStatus.APPROVED);
        save("MUTUAL_ONLY", MediaStatus.APPROVED);

        List<UserMedia> notMutual = userMediaRepository.findVisibleMediaForViewer(target.getId(), false, false, false);
        assertEquals(0, notMutual.size());

        List<UserMedia> mutual = userMediaRepository.findVisibleMediaForViewer(target.getId(), true, false, false);
        assertEquals(2, mutual.size());
    }

    @Test
    @DisplayName("VERIFIED_ONLY visible only when viewer verified")
    void verifiedOnlyOnlyWhenVerified() {
        save("VERIFIED_ONLY", MediaStatus.APPROVED);

        List<UserMedia> notVerified = userMediaRepository.findVisibleMediaForViewer(target.getId(), false, false, false);
        assertEquals(0, notVerified.size());

        List<UserMedia> verified = userMediaRepository.findVisibleMediaForViewer(target.getId(), false, true, false);
        assertEquals(1, verified.size());
    }

    @Test
    @DisplayName("EVENT_ONLY visible only when event active")
    void eventOnlyOnlyWhenEventActive() {
        save("EVENT_ONLY", MediaStatus.APPROVED);

        List<UserMedia> noEvent = userMediaRepository.findVisibleMediaForViewer(target.getId(), false, false, false);
        assertEquals(0, noEvent.size());

        List<UserMedia> eventActive = userMediaRepository.findVisibleMediaForViewer(target.getId(), false, false, true);
        assertEquals(1, eventActive.size());
    }

    @Test
    @DisplayName("Other users' media excluded")
    void otherUsersExcluded() {
        save("PUBLIC", MediaStatus.APPROVED);

        // another target user with PUBLIC media
        User other = new User();
        other.setFirstName("Other");
        other.setLastName("User");
        other.setEmail("other" + System.nanoTime() + "@example.com");
        other.setPhoneNumber(String.valueOf(System.nanoTime()));
        other.setPassword("pass");
        userRepository.save(other);
        UserMedia otherMedia = media(MediaType.PHOTO, "PUBLIC", "FULL_LENGTH", false, MediaStatus.APPROVED);
        otherMedia.setUser(other);
        userMediaRepository.save(otherMedia);

        List<UserMedia> result = userMediaRepository.findVisibleMediaForViewer(target.getId(), false, false, false);
        assertEquals(1, result.size());
        assertEquals(target.getId(), result.get(0).getUser().getId());
    }

    @Test
    @DisplayName("Combined flags - PUBLIC always + MUTUAL + VERIFIED + EVENT mix")
    void combinedFlags() {
        save("PUBLIC", MediaStatus.APPROVED);
        save("MUTUAL_ONLY", MediaStatus.APPROVED);
        save("VERIFIED_ONLY", MediaStatus.APPROVED);
        save("EVENT_ONLY", MediaStatus.APPROVED);

        // Viewer is mutual, verified, event active -> sees all 4
        List<UserMedia> all = userMediaRepository.findVisibleMediaForViewer(target.getId(), true, true, true);
        assertEquals(4, all.size());

        // Viewer is none of them -> sees only the PUBLIC one
        List<UserMedia> none = userMediaRepository.findVisibleMediaForViewer(target.getId(), false, false, false);
        assertEquals(1, none.size());
        assertEquals("PUBLIC", none.get(0).getPrivacyMode());
    }

    @Test
    @DisplayName("Video with requiresAccessApproval still gated at repo level (only APPROVED public visible unless flags)")
    void videoGatedMedia() {
        userMediaRepository.save(media(MediaType.VIDEO, "PUBLIC", "VIDEO", false, MediaStatus.APPROVED));
        List<UserMedia> result = userMediaRepository.findVisibleMediaForViewer(target.getId(), false, false, false);
        assertEquals(1, result.size());
        assertEquals(MediaType.VIDEO, result.get(0).getMediaType());
    }
}