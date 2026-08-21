package com.SocialPairly_Workflow_Manager.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Requires CSRF only for unsafe methods that authenticate via the HttpOnly auth cookie
 * and do <em>not</em> send a Bearer token.
 * <p>
 * Bearer-authenticated SPA calls (the common path) are unaffected, preserving existing clients.
 * Cookie-only sessions remain protected against cross-site form posts.
 */
public class CookieSessionCsrfRequestMatcher implements RequestMatcher {

    private final String authCookieName;
    private final RequestMatcher unsafeMethodMatcher = CsrfFilter.DEFAULT_CSRF_MATCHER;

    public CookieSessionCsrfRequestMatcher(String authCookieName) {
        this.authCookieName = (authCookieName == null || authCookieName.isBlank())
                ? AuthCookieService.DEFAULT_COOKIE_NAME
                : authCookieName;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if (request == null || !unsafeMethodMatcher.matches(request)) {
            return false;
        }
        if (hasBearerToken(request)) {
            return false;
        }
        return hasAuthCookie(request);
    }

    private static boolean hasBearerToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return auth != null
                && auth.regionMatches(true, 0, "Bearer ", 0, 7)
                && auth.length() > 7
                && !auth.substring(7).isBlank();
    }

    private boolean hasAuthCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (authCookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value != null && !value.isBlank();
            }
        }
        return false;
    }
}
