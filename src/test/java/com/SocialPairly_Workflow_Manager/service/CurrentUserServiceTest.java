package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private CurrentUserService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new CurrentUserService(userRepository);
        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUser - no authentication -> ResourceNotFound")
    void getCurrentUser_noAuth_throws() {
        assertThrows(ResourceNotFoundException.class, () -> service.getCurrentUser());
    }

    @Test
    @DisplayName("getCurrentUser - null auth name -> ResourceNotFound")
    void getCurrentUser_nullName_throws() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThrows(ResourceNotFoundException.class, () -> service.getCurrentUser());
    }

    @Test
    @DisplayName("getCurrentUser - auth but user not found -> ResourceNotFound")
    void getCurrentUser_userNotFound_throws() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("missing@example.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getCurrentUser());
    }

    @Test
    @DisplayName("getCurrentUser - success returns user")
    void getCurrentUser_success() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("john@example.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        assertEquals(user, service.getCurrentUser());
    }

    @Test
    @DisplayName("save - delegates to repository and returns saved user")
    void save_delegates() {
        when(userRepository.save(any(User.class))).thenReturn(user);
        assertEquals(user, service.save(user));
        verify(userRepository).save(user);
    }
}
