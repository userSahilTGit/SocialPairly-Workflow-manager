package com.SocialPairly_Workflow_Manager.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

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

    @Test
    void filterShouldContinueWhenNoToken() throws Exception {
        when(authCookieService.resolveToken(request)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void filterShouldContinueWhenTokenInvalid() throws Exception {
        when(authCookieService.resolveToken(request)).thenReturn("invalid-token");
        when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void filterShouldContinueWhenTokenRevoked() throws Exception {
        when(authCookieService.resolveToken(request)).thenReturn("revoked-token");
        when(jwtUtil.isTokenValid("revoked-token")).thenReturn(true);
        when(tokenBlacklistService.isRevoked("revoked-token")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }
}
