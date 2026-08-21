package com.SocialPairly_Workflow_Manager.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * Google Identity (OIDC) verifier used by {@code POST /api/auth/google}.
 * Extracted as a bean so integration tests can substitute a mock without
 * changing production verification behavior.
 */
@Configuration
public class GoogleAuthConfig {

    /** Web client ID from Google Cloud Console (public OAuth client id). */
    public static final String GOOGLE_CLIENT_ID =
            "1043168194153-i82qfvqg1jsk804qaa7ipkkov67b8pt4.apps.googleusercontent.com";

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        try {
            return new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    new GsonFactory()
            )
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create GoogleIdTokenVerifier", e);
        }
    }
}
