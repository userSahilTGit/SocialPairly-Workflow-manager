package com.SocialPairly_Workflow_Manager.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final ResourceLoader resourceLoader;

    @Value("${app.firebase.enabled:true}")
    private boolean enabled;

    @Value("${app.firebase.credentials-location:classpath:firebase-service-account.json}")
    private String credentialsLocation;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws IOException {
        if (!enabled) {
            log.info("Firebase Admin disabled via app.firebase.enabled=false");
            return;
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase Admin already initialized");
            return;
        }

        Resource credentials = resourceLoader.getResource(credentialsLocation);
        if (!credentials.exists()) {
            log.warn(
                    "Firebase credentials not found at {}. Skipping Firebase Admin init; "
                            + "phone verification via Firebase will be unavailable until the file is added.",
                    credentialsLocation);
            return;
        }

        try (InputStream serviceAccount = credentials.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin initialized from {}", credentialsLocation);
        }
    }
}
