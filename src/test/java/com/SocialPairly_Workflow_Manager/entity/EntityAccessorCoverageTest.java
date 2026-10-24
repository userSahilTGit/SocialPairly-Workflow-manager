package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityAccessorCoverageTest {

    @Test
    void loginSecurityEventAccessorsAndLifecycle() {
        LoginSecurityEvent event = new LoginSecurityEvent();
        event.onCreate();
        assertNotNull(event.getCreatedAt());
        event.setId(4L);
        event.setUserId(9L);
        event.setIdentifier("ada@example.com");
        event.setEventType(LoginSecurityEvent.FAILED_LOGIN);
        event.setSecurityTier(LoginSecurityEvent.TIER_SUBSCRIBED);
        event.setClientIp("127.0.0.1");
        event.setDetails("attempt=1");
        LocalDateTime created = LocalDateTime.now().minusMinutes(1);
        event.setCreatedAt(created);

        assertEquals(4L, event.getId());
        assertEquals(9L, event.getUserId());
        assertEquals("ada@example.com", event.getIdentifier());
        assertEquals(LoginSecurityEvent.FAILED_LOGIN, event.getEventType());
        assertEquals(LoginSecurityEvent.TIER_SUBSCRIBED, event.getSecurityTier());
        assertEquals("127.0.0.1", event.getClientIp());
        assertEquals("attempt=1", event.getDetails());
        assertEquals(created, event.getCreatedAt());
    }

    @Test
    void userAnswerQuestionOptionAndComplianceAccessors() {
        User user = new User();
        Question question = new Question();
        UserAnswer answer = new UserAnswer();
        answer.onCreate();
        answer.onUpdate();
        answer.setId(1L);
        answer.setUser(user);
        answer.setQuestion(question);
        answer.setAnswerValue("yes");
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        answer.setCreatedAt(created);
        answer.setUpdatedAt(created);
        assertEquals(1L, answer.getId());
        assertEquals(user, answer.getUser());
        assertEquals(question, answer.getQuestion());
        assertEquals("yes", answer.getAnswerValue());
        assertEquals(created, answer.getCreatedAt());
        assertEquals(created, answer.getUpdatedAt());

        QuestionOption option = new QuestionOption();
        option.setId(2L);
        option.setQuestion(question);
        option.setOptionText("Yes");
        option.setDisplayOrder(3);
        assertEquals(2L, option.getId());
        assertEquals(question, option.getQuestion());
        assertEquals("Yes", option.getOptionText());
        assertEquals(3, option.getDisplayOrder());

        UserIdentityCompliance compliance = new UserIdentityCompliance();
        compliance.onCreate();
        compliance.onUpdate();
        compliance.setId(8L);
        compliance.setUser(user);
        compliance.setDlFrontBlobId(11L);
        compliance.setDlBackBlobId(12L);
        compliance.setLastErrorCode("E");
        compliance.setDocumentsMeta("{}");
        compliance.setConsents("[]");
        compliance.setAuditEvents("[]");
        LocalDateTime stamp = LocalDateTime.now();
        compliance.setCreatedAt(stamp);
        compliance.setUpdatedAt(stamp);
        assertEquals(8L, compliance.getId());
        assertEquals(11L, compliance.getDlFrontBlobId());
        assertEquals(12L, compliance.getIdBackBlobId());
        assertEquals("E", compliance.getLastErrorCode());
        assertEquals("{}", compliance.getDocumentsMeta());
        assertEquals("[]", compliance.getConsents());
        assertEquals("[]", compliance.getAuditEvents());
        assertEquals(stamp, compliance.getCreatedAt());
        assertEquals(stamp, compliance.getUpdatedAt());
    }

    @Test
    void personalityAndProfileNullableCollections() {
        UserPersonalityProfile personality = new UserPersonalityProfile();
        personality.onCreate();
        personality.onUpdate();
        personality.setId(3L);
        User user = new User();
        personality.setUser(user);
        assertTrue(personality.getFriendDescriptors().isEmpty());
        personality.setFriendDescriptors(null);
        assertTrue(personality.getFriendDescriptors().isEmpty());
        personality.setFriendDescriptors(List.of("kind"));
        assertEquals(List.of("kind"), personality.getFriendDescriptors());
        assertTrue(personality.getPersonalityTraits().isEmpty());
        personality.setPersonalityTraits(null);
        assertTrue(personality.getPersonalityTraits().isEmpty());
        personality.setPersonalityTraits(Set.of("calm"));
        assertEquals(Set.of("calm"), personality.getPersonalityTraits());
        LocalDateTime stamp = LocalDateTime.now();
        personality.setCreatedAt(stamp);
        personality.setUpdatedAt(stamp);
        assertEquals(3L, personality.getId());
        assertEquals(user, personality.getUser());
        assertEquals(stamp, personality.getCreatedAt());
        assertEquals(stamp, personality.getUpdatedAt());

        UserProfile profile = new UserProfile();
        profile.setIdentityVerificationStatus("VERIFIED");
        profile.setBackgroundScreeningStatus("CLEAR");
        profile.setOnboardingStep("done");
        profile.setIdentityPage1CompletedAt(stamp);
        profile.setIdentityNameVerificationStatus("PASS");
        profile.setIdentityAgeVerificationStatus("PASS");
        profile.setIdentityPhotoVerificationStatus("PASS");
        assertTrue(profile.getInterests().isEmpty());
        profile.setInterests(null);
        assertTrue(profile.getInterests().isEmpty());
        profile.setInterests(Set.of("music"));
        assertEquals("VERIFIED", profile.getIdentityVerificationStatus());
        assertEquals("CLEAR", profile.getBackgroundScreeningStatus());
        assertEquals("done", profile.getOnboardingStep());
        assertEquals(stamp, profile.getIdentityPage1CompletedAt());
        assertEquals("PASS", profile.getIdentityNameVerificationStatus());
        assertEquals("PASS", profile.getIdentityAgeVerificationStatus());
        assertEquals("PASS", profile.getIdentityPhotoVerificationStatus());
        assertEquals(Set.of("music"), profile.getInterests());
        assertTrue(profile.getEducations().isEmpty());
    }
}
