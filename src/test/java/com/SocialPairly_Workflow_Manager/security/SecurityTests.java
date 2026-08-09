package com.SocialPairly_Workflow_Manager.security;

import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void customUserDetailsServiceShouldLoadExistingUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("encoded");
        user.setRole(Role.ADMIN);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("user@example.com");

        assertEquals("user@example.com", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void customUserDetailsServiceShouldThrowForUnknownUser() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("missing@example.com"));
    }

    @Test
    void jwtUtilShouldGenerateAndValidateToken() {
        JwtUtil jwtUtil = new JwtUtil("this-is-a-very-long-secret-key-for-jwt-123456", 60000L, 120000L);

        String token = jwtUtil.generateToken("demo@example.com");

        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("demo@example.com", jwtUtil.extractEmail(token));
    }
}
