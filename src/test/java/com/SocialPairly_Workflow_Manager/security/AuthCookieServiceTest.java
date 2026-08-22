package com.SocialPairly_Workflow_Manager.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AuthCookieServiceTest {

    private JwtUtil jwtUtil;
    private AuthCookieService cookieService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("01234567890123456789012345678901", 86_400_000L, 2_592_000_000L);
        cookieService = new AuthCookieService(jwtUtil, "SP_AUTH", false, "Lax");
    }

    @Test
    void writeAuthCookieShouldSetHttpOnlyCookieWithRememberMeMaxAge() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        cookieService.writeAuthCookie(response, "jwt-value", true);

        String header = response.getHeader("Set-Cookie");
        assertNotNull(header);
        assertTrue(header.contains("SP_AUTH=jwt-value"));
        assertTrue(header.toLowerCase().contains("httponly"));
        assertTrue(header.contains("Max-Age=2592000") || header.contains("Max-Age=2592000;"));
        assertTrue(header.contains("SameSite=Lax") || header.toLowerCase().contains("samesite=lax"));
        assertFalse(header.toLowerCase().contains("secure"));
    }

    @Test
    void writeAuthCookieShouldSetSecureWhenForwardedProtoIsHttps() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieService.writeAuthCookie(request, response, "jwt-value", false);

        String header = response.getHeader("Set-Cookie");
        assertNotNull(header);
        assertTrue(header.toLowerCase().contains("secure"));
    }

    @Test
    void writeAuthCookieShouldForceSecureWhenConfigured() {
        AuthCookieService secureService = new AuthCookieService(jwtUtil, "SP_AUTH", true, "Strict");
        MockHttpServletResponse response = new MockHttpServletResponse();

        secureService.writeAuthCookie(response, "jwt-value", false);

        String header = response.getHeader("Set-Cookie");
        assertNotNull(header);
        assertTrue(header.toLowerCase().contains("secure"));
        assertTrue(header.contains("SameSite=Strict") || header.toLowerCase().contains("samesite=strict"));
    }

    @Test
    void clearAuthCookieShouldExpireCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        cookieService.clearAuthCookie(response);

        String header = response.getHeader("Set-Cookie");
        assertNotNull(header);
        assertTrue(header.contains("Max-Age=0") || header.contains("Max-Age=0;"));
    }

    @Test
    void resolveTokenShouldPreferBearerOverCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bearer-token");
        request.setCookies(new Cookie("SP_AUTH", "cookie-token"));

        assertEquals("bearer-token", cookieService.resolveToken(request));
    }

    @Test
    void resolveTokenShouldFallBackToCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("SP_AUTH", "cookie-token"));

        assertEquals("cookie-token", cookieService.resolveToken(request));
    }
}
