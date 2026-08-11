package com.SocialPairly_Workflow_Manager.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseConfigTest {

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private Resource resource;

    @AfterEach
    void tearDown() {
        for (FirebaseApp app : List.copyOf(FirebaseApp.getApps())) {
            app.delete();
        }
    }

    @Test
    void initShouldSkipWhenDisabled() throws IOException {
        FirebaseConfig config = new FirebaseConfig(resourceLoader);
        ReflectionTestUtils.setField(config, "enabled", false);
        ReflectionTestUtils.setField(config, "credentialsLocation", "classpath:missing.json");

        assertDoesNotThrow(config::init);
        verify(resourceLoader, never()).getResource(anyString());
    }

    @Test
    void initShouldSkipWhenAlreadyInitialized() throws IOException {
        try (org.mockito.MockedStatic<FirebaseApp> firebaseApp = org.mockito.Mockito.mockStatic(FirebaseApp.class)) {
            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));

            FirebaseConfig config = new FirebaseConfig(resourceLoader);
            ReflectionTestUtils.setField(config, "enabled", true);
            ReflectionTestUtils.setField(config, "credentialsLocation", "classpath:missing.json");

            assertDoesNotThrow(config::init);
            verify(resourceLoader, never()).getResource(anyString());
        }
    }

    @Test
    void initShouldSkipWhenCredentialsMissing() throws IOException {
        when(resourceLoader.getResource("classpath:missing-firebase.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(false);

        FirebaseConfig config = new FirebaseConfig(resourceLoader);
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "credentialsLocation", "classpath:missing-firebase.json");

        assertDoesNotThrow(config::init);
        verify(resource, never()).getInputStream();
    }

    @Test
    void initShouldInitializeWhenCredentialsExist() throws Exception {
        when(resourceLoader.getResource("classpath:test-firebase.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        GoogleCredentials credentials = mock(GoogleCredentials.class);
        try (org.mockito.MockedStatic<GoogleCredentials> googleCredentials =
                     org.mockito.Mockito.mockStatic(GoogleCredentials.class);
             org.mockito.MockedStatic<FirebaseApp> firebaseApp = org.mockito.Mockito.mockStatic(FirebaseApp.class)) {
            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());
            googleCredentials.when(() -> GoogleCredentials.fromStream(any()))
                    .thenReturn(credentials);
            firebaseApp.when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)))
                    .thenReturn(mock(FirebaseApp.class));

            FirebaseConfig config = new FirebaseConfig(resourceLoader);
            ReflectionTestUtils.setField(config, "enabled", true);
            ReflectionTestUtils.setField(config, "credentialsLocation", "classpath:test-firebase.json");

            assertDoesNotThrow(config::init);
            firebaseApp.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)));
        }
    }
}
