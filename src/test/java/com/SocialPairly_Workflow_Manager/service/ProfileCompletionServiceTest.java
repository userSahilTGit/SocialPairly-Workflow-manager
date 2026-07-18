package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserAnswer;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileCompletionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserAnswerRepository answerRepository;

    @InjectMocks
    private ProfileCompletionService profileCompletionService;

    @Test
    void shouldReturnMinimalCompletionWhenProfileMissingAndNoActiveQuestions() {
        User user = new User();
        user.setId(1L);
        user.setProfileCompleted(false);

        when(questionRepository.findByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of());

        Map<String, Object> result = profileCompletionService.calculate(user, null);

        assertEquals(10, result.get("totalSections"));
        assertEquals(1, result.get("filledSections"));
        assertEquals(10, result.get("percentage"));
        assertFalse((Boolean) result.get("profileCompleted"));
    }

    @Test
    void shouldCalculateCompletionForPopulatedProfileAndPartialAnswers() {
        User user = new User();
        user.setId(2L);
        user.setProfileCompleted(true);

        UserProfile profile = new UserProfile();
        profile.setProfilePhotoUrl("/uploads/photo.png");
        profile.setAboutMe("About me");
        profile.setOccupation("Engineer");
        profile.setLifestyle("Active");
        profile.setLocationCity("Boston");
        profile.setLocationCountry("USA");
        profile.setDateOfBirth(LocalDate.of(1990, 1, 1));
        profile.setGender("Female");
        profile.setInterests(Set.of("music"));
        profile.setEducations(List.of(new com.SocialPairly_Workflow_Manager.entity.Education()));
        profile.getEducations().get(0).setInstitution("MIT");

        Question q1 = new Question();
        q1.setId(10L);
        Question q2 = new Question();
        q2.setId(20L);

        when(questionRepository.findByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(q1, q2));
        UserAnswer answer = new UserAnswer();
        answer.setQuestion(q1);
        answer.setAnswerValue("Yes");
        when(answerRepository.findByUserId(2L)).thenReturn(List.of(answer));

        Map<String, Object> result = profileCompletionService.calculate(user, profile);

        assertEquals(10, result.get("totalSections"));
        assertEquals(10, result.get("filledSections"));
        assertEquals(100, result.get("percentage"));
        assertTrue((Boolean) result.get("profileCompleted"));
    }
}
