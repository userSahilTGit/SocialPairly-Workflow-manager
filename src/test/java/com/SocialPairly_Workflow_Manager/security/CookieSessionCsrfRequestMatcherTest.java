package com.SocialPairly_Workflow_Manager.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class CookieSessionCsrfRequestMatcherTest {

    private final CookieSessionCsrfRequestMatcher matcher =
            new CookieSessionCsrfRequestMatcher("SP_AUTH");

    @Test
    void doesNotMatchSafeGet() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setCookies(new Cookie("SP_AUTH", "tok"));
        assertFalse(matcher.matches(request));
    }

    @Test
    void doesNotMatchWhenBearerPresentEvenWithAuthCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/profile");
        request.addHeader("Authorization", "Bearer abc");
        request.setCookies(new Cookie("SP_AUTH", "tok"));
        assertFalse(matcher.matches(request));
    }

    @Test
    void matchesCookieOnlyUnsafePost() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/profile");
        request.setCookies(new Cookie("SP_AUTH", "tok"));
        assertTrue(matcher.matches(request));
    }

    @Test
    void doesNotMatchUnauthenticatedPost() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        assertFalse(matcher.matches(request));
    }

    @Test
    void doesNotMatchBlankAuthCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/profile");
        request.setCookies(new Cookie("SP_AUTH", "  "));
        assertFalse(matcher.matches(request));
    }
}
