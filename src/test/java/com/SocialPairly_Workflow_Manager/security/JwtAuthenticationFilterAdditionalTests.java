package com.SocialPairly_Workflow_Manager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterAdditionalTests {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private JwtTokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateWhenTokenIsValid() throws Exception {
        when(authCookieService.resolveToken(request)).thenReturn("valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(tokenBlacklistService.isRevoked("valid-token")).thenReturn(false);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("user@example.com");

        UserDetails userDetails = new User("user@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("user@example.com", authentication.getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateFromCookieToken() throws Exception {
        when(authCookieService.resolveToken(request)).thenReturn("cookie-token");
        when(jwtUtil.isTokenValid("cookie-token")).thenReturn(true);
        when(tokenBlacklistService.isRevoked("cookie-token")).thenReturn(false);
        when(jwtUtil.extractEmail("cookie-token")).thenReturn("cookie@example.com");

        UserDetails userDetails = new User("cookie@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("cookie@example.com")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("cookie@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
