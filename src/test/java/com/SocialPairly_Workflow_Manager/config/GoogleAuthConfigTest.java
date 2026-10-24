package com.SocialPairly_Workflow_Manager.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class GoogleAuthConfigTest {

    @Test
    void buildsAVerifierForTheConfiguredClient() {
        GoogleIdTokenVerifier verifier = new GoogleAuthConfig().googleIdTokenVerifier();
        assertNotNull(verifier);
    }

    @Test
    void transportFailureBecomesAnIllegalStateException() {
        try (MockedStatic<GoogleNetHttpTransport> transport = mockStatic(GoogleNetHttpTransport.class)) {
            transport.when(GoogleNetHttpTransport::newTrustedTransport).thenThrow(new IOException("offline"));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> new GoogleAuthConfig().googleIdTokenVerifier());

            assertTrue(ex.getMessage().contains("Failed to create GoogleIdTokenVerifier"));
            assertTrue(ex.getCause() instanceof IOException);
        }
    }
}
