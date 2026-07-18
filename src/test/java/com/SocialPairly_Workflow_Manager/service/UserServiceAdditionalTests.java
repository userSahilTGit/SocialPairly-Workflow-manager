package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.DeleteAccountRequest;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
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
class UserServiceAdditionalTests {

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
    void deleteAccountShouldThrowWhenIdentifierNotFound() {
        DeleteAccountRequest request = new DeleteAccountRequest("missing@example.com", "pass");
        User currentUser = new User();
        currentUser.setId(1L);

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> userService.deleteAccount(currentUser, request));
    }

    @Test
    void deleteAccountShouldThrowWhenUserMismatch() {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.USER);
        user.setPassword("encoded");

        DeleteAccountRequest request = new DeleteAccountRequest("match@example.com", "pass");
        User currentUser = new User();
        currentUser.setId(1L);

        when(userRepository.findByEmail("match@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> userService.deleteAccount(currentUser, request));
    }

    @Test
    void findByIdentifierShouldThrowWhenMissing() {
        assertThrows(ResourceNotFoundException.class, () -> userService.findByIdentifier("missing@example.com"));
    }

    @Test
    void deleteAccountShouldDeleteProfileAndAnswers() {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.USER);
        user.setPassword("encoded");

        DeleteAccountRequest request = new DeleteAccountRequest("match@example.com", "pass");
        User currentUser = new User();
        currentUser.setId(2L);

        when(userRepository.findByEmail("match@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        when(answerRepository.findByUserId(2L)).thenReturn(java.util.List.of());
        when(profileRepository.findByUserId(2L)).thenReturn(Optional.of(new UserProfile()));

        userService.deleteAccount(currentUser, request);

        verify(answerRepository).deleteAll(anyList());
        verify(profileRepository).delete(any(UserProfile.class));
        verify(userRepository).delete(user);
    }
}
