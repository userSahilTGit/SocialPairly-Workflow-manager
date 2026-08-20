package com.SocialPairly_Workflow_Manager.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Issues and clears the HttpOnly auth cookie used for Remember Me / session persistence.
 * Bearer tokens remain supported for backward compatibility.
 */
@Service
public class AuthCookieService {

    public static final String DEFAULT_COOKIE_NAME = "SP_AUTH";

    private final JwtUtil jwtUtil;
    private final String cookieName;
    private final boolean secure;
    private final String sameSite;

    public AuthCookieService(
            JwtUtil jwtUtil,
            @Value("${app.auth.cookie.name:SP_AUTH}") String cookieName,
            @Value("${app.auth.cookie.secure:false}") boolean secure,
            @Value("${app.auth.cookie.same-site:Lax}") String sameSite
    ) {
        this.jwtUtil = jwtUtil;
        this.cookieName = (cookieName == null || cookieName.isBlank()) ? DEFAULT_COOKIE_NAME : cookieName;
        this.secure = secure;
        this.sameSite = (sameSite == null || sameSite.isBlank()) ? "Lax" : sameSite;
    }

    public void writeAuthCookie(HttpServletResponse response, String token, boolean rememberMe) {
        if (response == null || token == null || token.isBlank()) {
            return;
        }
        long maxAgeSeconds = Math.max(1L, jwtUtil.resolveExpirationMs(rememberMe) / 1000L);
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAuthCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String readToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                return (value == null || value.isBlank()) ? null : value;
            }
        }
        return null;
    }

    /** Prefer Authorization Bearer, fall back to the auth cookie. */
    public String resolveToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            String bearer = authHeader.substring(7).trim();
            if (!bearer.isEmpty()) {
                return bearer;
            }
        }
        return readToken(request);
    }

    public String getCookieName() {
        return cookieName;
    }
}
