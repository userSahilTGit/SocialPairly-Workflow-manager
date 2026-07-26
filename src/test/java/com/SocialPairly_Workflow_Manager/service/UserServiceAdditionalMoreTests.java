package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.DeleteAccountRequest;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceAdditionalMoreTests {

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
    void shouldRejectAdminDeleteAccount() {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        admin.setPassword("encoded");

        DeleteAccountRequest request = new DeleteAccountRequest("admin@example.com", "pass");
        User currentUser = new User();
        currentUser.setId(2L);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.deleteAccount(currentUser, request));
    }

    @Test
    void shouldRejectDeleteWhenPasswordDoesNotMatch() {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.USER);
        user.setPassword("encoded");

        DeleteAccountRequest request = new DeleteAccountRequest("match@example.com", "wrongpass");
        User currentUser = new User();
        currentUser.setId(2L);

        when(userRepository.findByEmail("match@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.deleteAccount(currentUser, request));
    }
}
