package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControllerTests {

    @Mock
    private AuthService authService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuthRateLimitService authRateLimitService;

    @Mock
    private AdminService adminService;

    @Mock
    private QuestionService questionService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private RefundService refundService;

    @Mock
    private PlanUpgradeService planUpgradeService;

    @Mock
    private ProfileService profileService;

    @Mock
    private UserMediaService userMediaService;

    @Mock
    private ProfileCompletionService profileCompletionService;

    @Mock
    private AnswerService answerService;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserTokenService userTokenService;

    @InjectMocks
    private AuthController authController;

    @InjectMocks
    private AdminController adminController;

    @InjectMocks
    private ProfileController profileController;

    @InjectMocks
    private QuestionController questionController;

    @InjectMocks
    private UserController userController;

    @Test
    void authControllerShouldDelegateRegistrationAndLogin() {
        RegisterRequest registerRequest = new RegisterRequest("A", "B", "a@example.com", "1234567", "password", "password", "addr", true, true, true, true, false);
        LoginRequest loginRequest = new LoginRequest("a@example.com", "password", null);
        UserDto userDto = new UserDto(1L, "A", "B", null, "A", "a@example.com", "1234567", "addr", null, false, false, false, false, false, false, false, false, false, null, false, "", 0);
        AuthResponse authResponse = new AuthResponse("token", userDto);

        when(authService.register(registerRequest)).thenReturn(authResponse);
        when(authService.login(loginRequest)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> registerResponse = authController.register(registerRequest);
        ResponseEntity<AuthResponse> loginResponse = authController.login(loginRequest, new MockHttpServletRequest());

        assertEquals(200, registerResponse.getStatusCode().value());
        assertSame(authResponse, registerResponse.getBody());
        assertSame(authResponse, loginResponse.getBody());
        verify(authRateLimitService).check(eq(AuthRateLimitService.ACTION_LOGIN), any(), eq("a@example.com"));
    }

    @Test
    void adminControllerShouldExposeAdminEndpoints() {
        AdminStatsDto stats = new AdminStatsDto(
                1,
                1,
                1,
                0,
                1,
                2,
                List.<AdminStatsDto.CountByLabel>of(),
                List.<AdminStatsDto.CountByLabel>of(),
                List.<AdminStatsDto.CountByLabel>of(),
                Map.of()
        );
        UserDto userDto = new UserDto(1L, "A", "B", null, "A", "a@example.com", "123", "addr", null, false, false, false, false, false, false, false, false, false, null, false, "", 0);
        Question question = new Question();
        QuestionRequest request = new QuestionRequest("q", null, "cat", true, true, List.of());

        List<UserDto> userDtos = List.of(userDto);
        List<Question> questions = List.of(question);

        when(adminService.getStats()).thenReturn(stats);
        when(adminService.listUsers()).thenReturn(userDtos);
        when(questionService.findAll()).thenReturn(questions);
        when(questionService.create(request)).thenReturn(question);
        when(questionService.update(1L, request)).thenReturn(question);

        ResponseEntity<AdminStatsDto> statsResponse = adminController.getStats();
        ResponseEntity<List<UserDto>> usersResponse = adminController.getUsers();
        ResponseEntity<List<Question>> questionsResponse = adminController.getAllQuestions();
        ResponseEntity<Question> createdResponse = adminController.createQuestion(request);
        ResponseEntity<Question> updatedResponse = adminController.updateQuestion(1L, request);
        ResponseEntity<Map<String, String>> deletedResponse = adminController.deleteQuestion(1L);

        assertEquals(200, statsResponse.getStatusCode().value());
        assertEquals(1, usersResponse.getBody().size());
        assertEquals(1, questionsResponse.getBody().size());
        assertSame(question, createdResponse.getBody());
        assertSame(question, updatedResponse.getBody());
        assertEquals("Question deleted", deletedResponse.getBody().get("message"));
    }

    @Test
    void profileControllerShouldReturnAndUpdateProfileData() {
        User user = new User();
        user.setId(1L);
        user.setEmail("a@example.com");
        com.SocialPairly_Workflow_Manager.entity.UserProfile profile = new com.SocialPairly_Workflow_Manager.entity.UserProfile();
        profile.setAboutMe("about");
        profile.setOccupation("job");
        ProfileRequest request = new ProfileRequest("about", "job", "life", "city", "country", 1.2, 2.3, null, "M", null, null, null, null);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "data".getBytes());

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(profileService.getProfile(user)).thenReturn(profile);
        when(profileService.updateProfile(user, request)).thenReturn(profile);
        when(profileCompletionService.calculate(user, profile)).thenReturn(Map.of("percent", 100));
        when(userMediaService.uploadProfilePhoto(user, file)).thenReturn(
                MediaUploadResponseDto.builder().id(9L).mediaUrl("/api/media/9/stream").build());

        ResponseEntity<Map<String, Object>> profileResponse = profileController.getProfile();
        ResponseEntity<Map<String, Object>> completionResponse = profileController.getProfileCompletion();
        ResponseEntity<ProfileDto> updateResponse = profileController.updateProfile(request);
        ResponseEntity<Map<String, String>> uploadResponse = profileController.uploadPhoto(file);

        assertEquals(200, profileResponse.getStatusCode().value());
        assertTrue(profileResponse.getBody().containsKey("user"));
        assertTrue(profileResponse.getBody().containsKey("profile"));
        assertEquals(100, completionResponse.getBody().get("percent"));
        assertNotNull(updateResponse.getBody());
        assertEquals("about", updateResponse.getBody().aboutMe());
        assertEquals("job", updateResponse.getBody().occupation());
        assertEquals("/api/media/9/stream", uploadResponse.getBody().get("url"));
        verify(profileService).setProfilePhoto(user, "/api/media/9/stream");
    }

    @Test
    void questionControllerShouldReturnQuestionsAndSaveAnswers() {
        User user = new User();
        user.setId(1L);
        QuestionDto questionDto = new QuestionDto(1L, "Q", null, "cat", true, true, List.of(), null);
        AnswerRequest answerRequest = new AnswerRequest(1L, "value");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(questionService.getActiveQuestionsForUser(user)).thenReturn(List.of(questionDto));

        ResponseEntity<List<QuestionDto>> questionsResponse = questionController.getQuestions();
        ResponseEntity<Map<String, String>> answersResponse = questionController.submitAnswers(List.of(answerRequest));

        assertEquals(200, questionsResponse.getStatusCode().value());
        assertEquals(1, questionsResponse.getBody().size());
        assertEquals("Answers saved", answersResponse.getBody().get("message"));
        verify(answerService).saveAnswers(eq(user), anyList());
    }

    @Test
    void userControllerShouldReturnCurrentUserAndDeleteAccount() {
        User user = new User();
        user.setId(2L);
        user.setEmail("b@example.com");
        DeleteAccountRequest deleteRequest = new DeleteAccountRequest("password");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(subscriptionService.findPrimaryActiveSubscription(2L)).thenReturn(java.util.Optional.empty());
        when(userProfileRepository.findByUserId(2L)).thenReturn(java.util.Optional.empty());

        ResponseEntity<UserDto> meResponse = userController.me();
        ResponseEntity<Map<String, String>> deleteResponse = userController.deleteAccount(deleteRequest);

        assertEquals(200, meResponse.getStatusCode().value());
        assertEquals("b@example.com", meResponse.getBody().email());
        assertEquals("Account deleted successfully", deleteResponse.getBody().get("message"));
        verify(userService).deleteAccount(eq(user), eq(deleteRequest));
    }

    @Test
    void adminControllerShouldExposePaymentSubscriptionAndRefundEndpoints() {
        AdminPaymentDto paymentDto = new AdminPaymentDto(
                1L, 1L, "User One", "user@example.com", "ch_1",
                new BigDecimal("29.99"), "usd", "succeeded", LocalDateTime.now(), null
        );
        AdminSubscriptionDto subscriptionDto = new AdminSubscriptionDto(
                2L, 1L, "User One", 5L, "Pro",
                LocalDateTime.now(), LocalDateTime.now().plusDays(30), "active"
        );
        AdminRefundListDto refundListDto = new AdminRefundListDto(
                3L, "REF-3", new BigDecimal("29.99"), "Initiated", "Requested", LocalDateTime.now()
        );
        AdminRefundDetailDto refundDetailDto = new AdminRefundDetailDto(
                3L, "REF-3", "User One", "user@example.com", new BigDecimal("29.99"),
                "Initiated", "Requested", "reason", null, LocalDateTime.now(), null
        );

        when(adminService.listPayments()).thenReturn(List.of(paymentDto));
        when(subscriptionService.getAllSubscriptionsForAdmin()).thenReturn(List.of(subscriptionDto));
        when(refundService.listRefundsForAdmin()).thenReturn(List.of(refundListDto));
        when(refundService.getRefundDetailForAdmin(3L)).thenReturn(refundDetailDto);
        when(refundService.adminRequestCall(3L)).thenReturn(refundDetailDto);
        when(refundService.adminApprove(3L)).thenReturn(refundDetailDto);
        when(refundService.adminClose(3L)).thenReturn(refundDetailDto);
        when(refundService.adminCompletePayout(3L)).thenReturn(refundDetailDto);

        assertEquals(1, adminController.getPayments().getBody().size());
        assertEquals(1, adminController.getSubscriptions().getBody().size());
        assertEquals("Subscription removed successfully",
                adminController.removeSubscription(2L).getBody().get("message"));
        assertEquals(1, adminController.getRefunds().getBody().size());
        assertSame(refundDetailDto, adminController.getRefundDetail(3L).getBody());
        assertSame(refundDetailDto, adminController.requestCall(3L).getBody());
        assertSame(refundDetailDto, adminController.approveRefund(3L).getBody());
        assertSame(refundDetailDto, adminController.closeRefund(3L).getBody());
        assertSame(refundDetailDto, adminController.completeRefund(3L).getBody());

        verify(subscriptionService).removeSubscription(2L);
    }
}
