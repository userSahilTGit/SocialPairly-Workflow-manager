package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.DeleteAccountRequest;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private UserAnswerRepository answerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldDeleteAccountForValidUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPhoneNumber("+12025550123");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);

        DeleteAccountRequest request = new DeleteAccountRequest("test@example.com", "secret123");

        when(userRepository.findByEmail(eq("test@example.com"))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("secret123"), eq("encoded-password"))).thenReturn(true);
        when(answerRepository.findByUserId(eq(1L))).thenReturn(Collections.emptyList());
        when(profileRepository.findByUserId(eq(1L))).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userService.deleteAccount(user, request));

        verify(answerRepository).deleteAll(any());
        verify(profileRepository, never()).delete(any());
        verify(userRepository).delete(user);
    }

    @Test
    void shouldPreventAdminDeletion() {
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPhoneNumber("+12025550123");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);

        DeleteAccountRequest request = new DeleteAccountRequest("admin@example.com", "secret123");

        when(userRepository.findByEmail(eq("admin@example.com"))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("secret123"), eq("encoded-password"))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.deleteAccount(user, request));
        verify(userRepository, never()).delete(any());
    }
}
