package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityTests {

    @Test
    void questionAndOptionShouldSupportLifecycleAndRelations() {
        Question question = new Question();
        question.setQuestionText("What is your favorite color?");
        question.setType(QuestionType.SINGLE_CHOICE);
        question.setCategory("Lifestyle");
        question.setRequired(true);
        question.setActive(true);
        question.onCreate();

        QuestionOption option = new QuestionOption();
        option.setOptionText("Blue");
        option.setDisplayOrder(1);
        option.setQuestion(question);
        question.getOptions().add(option);

        assertEquals("What is your favorite color?", question.getQuestionText());
        assertEquals(1, question.getOptions().size());
        assertNotNull(question.getCreatedAt());
        assertEquals("Blue", question.getOptions().get(0).getOptionText());
    }

    @Test
    void userAndProfileShouldTrackTimestamps() {
        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPhoneNumber("1111111");
        user.setPassword("pw");
        user.setAddress("Paris");
        user.setRole(Role.USER);
        user.onCreate();

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setAboutMe("Hello");
        profile.setOccupation("Engineer");
        profile.setLocationCity("Paris");
        profile.setLocationCountry("France");
        profile.setDateOfBirth(LocalDate.of(1990, 1, 1));
        profile.setGender("F");
        profile.setInterests(Set.of("music", "travel"));
        profile.onCreate();

        assertEquals("Jane", user.getFirstName());
        assertTrue(user.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)) || user.getCreatedAt().equals(LocalDateTime.now()));
        assertEquals("Hello", profile.getAboutMe());
        assertEquals(2, profile.getInterests().size());
    }

    @Test
    void educationAndUserAnswerShouldStoreRelatedData() {
        Education education = new Education();
        education.setInstitution("MIT");
        education.setDegree("BSc");
        education.setFieldOfStudy("CS");
        education.setStartYear(2010);
        education.setEndYear(2014);

        UserAnswer answer = new UserAnswer();
        answer.setAnswerValue("selected");
        answer.onCreate();

        assertEquals("MIT", education.getInstitution());
        assertEquals("selected", answer.getAnswerValue());
        assertNotNull(answer.getCreatedAt());
    }

    @Test
    void fullEntityGettersAndSettersShouldWork() {
        User user = new User();
        user.setId(123L);
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail("alice@example.com");
        user.setPhoneNumber("5551234");
        user.setPassword("pass");
        user.setAddress("Wonderland");
        user.setRole(Role.USER);
        user.setProfileCompleted(true);

        UserProfile profile = new UserProfile();
        profile.setId(10L);
        profile.setUser(user);
        profile.setProfilePhotoUrl("/uploads/photo.png");
        profile.setAboutMe("About Alice");
        profile.setOccupation("Explorer");
        profile.setLifestyle("Adventurous");
        profile.setLocationCity("London");
        profile.setLocationCountry("UK");
        profile.setLatitude(51.5);
        profile.setLongitude(-0.1);
        profile.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        profile.setGender("F");
        profile.setInterests(java.util.Set.of("music", "travel"));

        Education education = new Education();
        education.setId(20L);
        education.setProfile(profile);
        education.setInstitution("Oxford");
        education.setDegree("BA");
        education.setFieldOfStudy("Philosophy");
        education.setStartYear(2008);
        education.setEndYear(2011);
        profile.getEducations().add(education);

        QuestionOption option = new QuestionOption();
        option.setId(30L);
        option.setOptionText("Option A");
        option.setDisplayOrder(2);

        UserAnswer answer = new UserAnswer();
        answer.setId(40L);
        answer.setUser(user);
        answer.setQuestion(new Question());
        answer.setAnswerValue("Yes");
        answer.onCreate();

        assertEquals(123L, user.getId());
        assertEquals("Alice", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("alice@example.com", user.getEmail());
        assertTrue(user.isProfileCompleted());

        assertEquals("/uploads/photo.png", profile.getProfilePhotoUrl());
        assertEquals("Explorer", profile.getOccupation());
        assertEquals(2, profile.getInterests().size());
        assertEquals("Oxford", profile.getEducations().get(0).getInstitution());

        assertEquals("Option A", option.getOptionText());
        assertEquals(2, option.getDisplayOrder());

        assertEquals("Yes", answer.getAnswerValue());
        assertNotNull(answer.getCreatedAt());
    }

    @Test
    void shouldInitializeEntityTimestampsForUserProfileAndUserAnswer() {
        User user = new User();
        user.onCreate();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());

        user.onUpdate();
        assertNotNull(user.getUpdatedAt());

        UserProfile profile = new UserProfile();
        profile.onCreate();
        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());

        profile.onUpdate();
        assertNotNull(profile.getUpdatedAt());

        UserAnswer answer = new UserAnswer();
        answer.onCreate();
        assertNotNull(answer.getCreatedAt());
        assertNotNull(answer.getUpdatedAt());

        answer.onUpdate();
        assertNotNull(answer.getUpdatedAt());
    }

    @Test
    void shouldSupportEducationAndQuestionOptionProperties() {
        Education education = new Education();
        education.setInstitution("Harvard");
        education.setDegree("MBA");
        education.setFieldOfStudy("Business");
        education.setStartYear(2015);
        education.setEndYear(2017);

        QuestionOption option = new QuestionOption();
        option.setOptionText("Apple");
        option.setDisplayOrder(5);

        assertEquals("Harvard", education.getInstitution());
        assertEquals("MBA", education.getDegree());
        assertEquals(2017, education.getEndYear());
        assertEquals("Apple", option.getOptionText());
        assertEquals(5, option.getDisplayOrder());
    }

    @Test
    void shouldExerciseUserAndUserProfileGettersAndSetters() {
        User user = new User();
        user.setId(99L);
        user.setFirstName("Charlie");
        user.setLastName("Brown");
        user.setEmail("charlie@example.com");
        user.setPhoneNumber("1234567");
        user.setPassword("topsecret");
        user.setAddress("Peanuts Street");
        user.setRole(Role.USER);
        user.setProfileCompleted(true);

        UserProfile profile = new UserProfile();
        profile.setId(77L);
        profile.setUser(user);
        profile.setProfilePhotoUrl("/uploads/charlie.png");
        profile.setAboutMe("Good grief");
        profile.setOccupation("Cartoonist");
        profile.setLifestyle("Creative");
        profile.setLocationCity("Minneapolis");
        profile.setLocationCountry("USA");
        profile.setLatitude(44.98);
        profile.setLongitude(-93.26);
        profile.setDateOfBirth(java.time.LocalDate.of(1980, 10, 1));
        profile.setGender("M");
        profile.setInterests(java.util.Set.of("drawing", "stories"));

        Education education = new Education();
        education.setProfile(profile);
        education.setInstitution("Art School");
        profile.getEducations().add(education);

        assertEquals(99L, user.getId());
        assertEquals("Charlie", user.getFirstName());
        assertEquals("Brown", user.getLastName());
        assertEquals("charlie@example.com", user.getEmail());
        assertTrue(user.isProfileCompleted());

        assertEquals(77L, profile.getId());
        assertEquals("/uploads/charlie.png", profile.getProfilePhotoUrl());
        assertEquals("Good grief", profile.getAboutMe());
        assertEquals(2, profile.getInterests().size());
        assertEquals("Art School", profile.getEducations().get(0).getInstitution());
        assertEquals(user, profile.getUser());
    }
}
