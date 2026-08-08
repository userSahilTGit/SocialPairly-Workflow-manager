package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DtoTests {

    @Test
    void recordsShouldExposeSubmittedValues() {
        RegisterRequest register = new RegisterRequest("Ada", "Lovelace", "ada@example.com", "1234567", "secret", "London");
        LoginRequest login = new LoginRequest("ada@example.com", "secret");
        AnswerRequest answer = new AnswerRequest(10L, "yes");
        DeleteAccountRequest deleteAccount = new DeleteAccountRequest("secret");
        ForgotPasswordSendOtpRequest sendOtp = new ForgotPasswordSendOtpRequest("ada@example.com");
        ForgotPasswordVerifyOtpRequest verifyOtp = new ForgotPasswordVerifyOtpRequest("ada@example.com", "222222");
        ForgotPasswordResetRequest reset = new ForgotPasswordResetRequest("ada@example.com", "222222", "newSecret", "newSecret");
        ProfileRequest profile = new ProfileRequest("about", "Engineer", "balanced", "Berlin", "Germany", 52.5, 13.4, LocalDate.of(1990, 1, 1), "F", Set.of("music"), List.of(new ProfileRequest.EducationDto("MIT", "BSc", "CS", 2010, 2014)));
        QuestionRequest questionRequest = new QuestionRequest("Question", QuestionType.SINGLE_CHOICE, "Lifestyle", true, true, List.of("A", "B"));

        assertEquals("Ada", register.firstName());
        assertEquals("ada@example.com", login.identifier());
        assertEquals(10L, answer.questionId());
        assertEquals("secret", deleteAccount.password());
        assertEquals("222222", verifyOtp.otp());
        assertEquals("newSecret", reset.newPassword());
        assertEquals("Engineer", profile.occupation());
        assertEquals("music", profile.interests().iterator().next());
        assertEquals("Question", questionRequest.questionText());
    }

    @Test
    void userAndQuestionDtosShouldBeCreatedFromEntities() {
        User user = new User();
        user.setId(7L);
        user.setFirstName("Grace");
        user.setLastName("Hopper");
        user.setEmail("grace@example.com");
        user.setPhoneNumber("7654321");
        user.setAddress("New York");
        user.setRole(Role.ADMIN);
        user.setProfileCompleted(true);

        UserDto userDto = UserDto.from(user);
        assertEquals("Grace", userDto.firstName());
        assertEquals(Role.ADMIN, userDto.role());
        assertTrue(userDto.profileCompleted());

        Question question = new Question();
        question.setId(11L);
        question.setQuestionText("How do you feel?");
        question.setType(QuestionType.TEXT);
        question.setCategory("Mood");
        question.setRequired(true);
        question.setActive(true);

        QuestionDto questionDto = QuestionDto.from(question, "ok");
        assertEquals("How do you feel?", questionDto.questionText());
        assertEquals("ok", questionDto.answerValue());
    }

    @Test
    void adminStatsDtoCountByLabelShouldExposeProperties() {
        AdminStatsDto.CountByLabel count = new AdminStatsDto.CountByLabel("Today", 3);
        assertEquals("Today", count.label());
        assertEquals(3, count.count());
    }
}
