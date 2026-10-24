package com.SocialPairly_Workflow_Manager.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SpaCsrfTokenRequestHandlerTest {

    @Test
    void rawHeaderIsAcceptedAndMissingHeaderUsesTheXorHandler() {
        SpaCsrfTokenRequestHandler handler = new SpaCsrfTokenRequestHandler();
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "csrf-value");
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, () -> token);

        MockHttpServletRequest withHeader = new MockHttpServletRequest();
        withHeader.addHeader("X-XSRF-TOKEN", "csrf-value");
        assertEquals("csrf-value", handler.resolveCsrfTokenValue(withHeader, token));

        String xorResolved = handler.resolveCsrfTokenValue(new MockHttpServletRequest(), token);
        assertNotEquals("csrf-value", xorResolved);
    }
}
