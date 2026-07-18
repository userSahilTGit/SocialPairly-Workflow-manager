package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @Test
    void userRepositoryShouldSupportCrudAndLookups() {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setPhoneNumber("9999999");
        user.setPassword("pw");
        user.setAddress("Earth");
        user.setRole(Role.USER);
        user.setProfileCompleted(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertTrue(found.isPresent());
        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertEquals(1L, userRepository.countByRole(Role.USER));
    }

    @Test
    void questionRepositoryShouldFindActiveQuestions() {
        Question question = new Question();
        question.setQuestionText("Favorite movie?");
        question.setType(QuestionType.TEXT);
        question.setCategory("Hobby");
        question.setRequired(true);
        question.setActive(true);
        entityManager.persistAndFlush(question);

        List<Question> questions = questionRepository.findByActiveTrueOrderByCreatedAtAsc();
        assertFalse(questions.isEmpty());
        assertEquals("Favorite movie?", questions.get(0).getQuestionText());
    }

    @Test
    void profileAndAnswerRepositoriesShouldWorkTogether() {
        User user = new User();
        user.setFirstName("Profile");
        user.setLastName("Owner");
        user.setEmail("profile@example.com");
        user.setPhoneNumber("7777777");
        user.setPassword("pw");
        user.setAddress("Mars");
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(user);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setAboutMe("Hello");
        profile.setLocationCity("Paris");
        profile.setLocationCountry("France");
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(profile);

        Question question = new Question();
        question.setQuestionText("What do you like?");
        question.setType(QuestionType.SINGLE_CHOICE);
        question.setCategory("Interest");
        question.setRequired(true);
        question.setActive(true);
        entityManager.persistAndFlush(question);

        UserAnswer answer = new UserAnswer();
        answer.setUser(user);
        answer.setQuestion(question);
        answer.setAnswerValue("Music");
        entityManager.persistAndFlush(answer);

        Optional<UserProfile> foundProfile = userProfileRepository.findByUserId(user.getId());
        assertTrue(foundProfile.isPresent());
        assertEquals("Hello", foundProfile.get().getAboutMe());

        List<UserAnswer> answers = userAnswerRepository.findByUserId(user.getId());
        assertEquals(1, answers.size());
        assertEquals(1L, userAnswerRepository.countByQuestionId(question.getId()));
    }
}
