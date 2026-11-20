package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.UpdatePhoneRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.service.AuthRateLimitService;
import com.SocialPairly_Workflow_Manager.service.EmployeeAccessService;
import com.SocialPairly_Workflow_Manager.service.SubscriptionService;
import com.SocialPairly_Workflow_Manager.service.UserService;
import com.SocialPairly_Workflow_Manager.service.UserTokenService;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerAdditionalTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UserTokenService userTokenService;

    @Mock
    private EmployeeAccessService employeeAccessService;

    @Mock
    private AuthRateLimitService authRateLimitService;

    @InjectMocks
    private UserController userController;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(5L);
        user.setEmail("user@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        lenient().when(employeeAccessService.applyEmployeeAccessOnAuth(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(employeeAccessService.toEnrichedDto(any(User.class), any()))
                .thenAnswer(invocation -> UserDto.from(invocation.getArgument(0), invocation.getArgument(1)));
    }

    @Test
    void meShouldCreditSubscriptionTokensWhenActiveSubscriptionExists() {
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        Plan plan = new Plan();
        plan.setId(2L);
        subscription.setPlan(plan);

        UserProfile profile = new UserProfile();
        when(subscriptionService.findPrimaryActiveSubscription(5L)).thenReturn(Optional.of(subscription));
        when(userProfileRepository.findByUserId(5L)).thenReturn(Optional.of(profile));

        ResponseEntity<UserDto> response = userController.me();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("user@example.com", response.getBody().email());
        verify(userTokenService).ensureSubscriptionTokensCredited(user, subscription);
    }

    @Test
    void meShouldReturnUserWithoutProfile() {
        when(subscriptionService.findPrimaryActiveSubscription(5L)).thenReturn(Optional.empty());
        when(userProfileRepository.findByUserId(5L)).thenReturn(Optional.empty());

        ResponseEntity<UserDto> response = userController.me();

        assertNotNull(response.getBody());
        verify(userTokenService, never()).ensureSubscriptionTokensCredited(any(), any());
    }

    @Test
    void updatePhoneShouldDelegateToUserService() {
        UpdatePhoneRequest request = new UpdatePhoneRequest("+12025550123");
        when(userService.updatePhone(user, "+12025550123", null))
                .thenReturn(Map.of("phoneNumber", "+12025550123", "verified", false));

        ResponseEntity<Map<String, Object>> response = userController.updatePhone(request, new MockHttpServletRequest());

        assertEquals("+12025550123", response.getBody().get("phoneNumber"));
        verify(userService).updatePhone(user, "+12025550123", null);
    }

    @Test
    void updatePhoneWithOtpShouldBeRateLimitedAndDelegate() {
        UpdatePhoneRequest request = new UpdatePhoneRequest("+12025550123", "1234");
        when(userService.updatePhone(user, "+12025550123", "1234")).thenReturn(Map.of("message", "ok"));

        userController.updatePhone(request, new MockHttpServletRequest());

        verify(authRateLimitService).check(eq(AuthRateLimitService.ACTION_PHONE_CHANGE_VERIFY), anyString(), eq("5"));
        verify(userService).updatePhone(user, "+12025550123", "1234");
    }

    @Test
    void sendPhoneChangeOtpShouldBeRateLimitedAndDelegate() {
        when(userService.sendPhoneChangeOtp(user)).thenReturn(Map.of("message", "sent"));

        ResponseEntity<Map<String, String>> response = userController.sendPhoneChangeOtp(new MockHttpServletRequest());

        assertEquals("sent", response.getBody().get("message"));
        verify(authRateLimitService).check(eq(AuthRateLimitService.ACTION_PHONE_CHANGE_SEND_OTP), anyString(), eq("5"));
    }
}
