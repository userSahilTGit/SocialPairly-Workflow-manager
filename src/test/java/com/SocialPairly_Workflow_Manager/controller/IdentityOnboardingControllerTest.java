package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundRequest;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundResponse;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.IdentityOnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityOnboardingControllerTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private IdentityOnboardingService identityOnboardingService;

    private IdentityOnboardingController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new IdentityOnboardingController(currentUserService, identityOnboardingService);
        user = new User();
        user.setId(7L);
        lenient().when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getIdentityDelegates() {
        IdentityBackgroundResponse body = mock(IdentityBackgroundResponse.class);
        when(identityOnboardingService.getIdentity(user)).thenReturn(body);
        ResponseEntity<IdentityBackgroundResponse> response = controller.getIdentity();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(body, response.getBody());
    }

    @Test
    void saveIdentityDelegates() {
        IdentityBackgroundRequest request = mock(IdentityBackgroundRequest.class);
        when(request.action()).thenReturn("CONTINUE");
        IdentityBackgroundResponse body = mock(IdentityBackgroundResponse.class);
        when(identityOnboardingService.saveIdentity(user, request)).thenReturn(body);
        ResponseEntity<IdentityBackgroundResponse> response = controller.saveIdentity(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(body, response.getBody());
    }

    @Test
    void getReferenceDataDelegates() {
        when(identityOnboardingService.getReferenceData()).thenReturn(Map.of("ok", true));
        ResponseEntity<Map<String, Object>> response = controller.getReferenceData();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("ok"));
    }

    @Test
    void uploadDocumentDelegates() {
        MockMultipartFile file = new MockMultipartFile("file", "dl.jpg", "image/jpeg", new byte[]{1});
        when(identityOnboardingService.uploadDocument(user, file, "DL_FRONT")).thenReturn(Map.of("id", 9L));
        ResponseEntity<Map<String, Object>> response = controller.uploadDocument(file, "DL_FRONT");
        assertEquals(9L, response.getBody().get("id"));
    }

    @Test
    void streamDocumentReturnsBytesAndContentType() {
        when(identityOnboardingService.streamDocument(user, 3L))
                .thenReturn(new IdentityOnboardingService.DocumentStreamResult("image/jpeg", new byte[]{9, 8}));
        ResponseEntity<byte[]> response = controller.streamDocument(3L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(new byte[]{9, 8}, response.getBody());
        assertEquals("image/jpeg", response.getHeaders().getContentType().toString());
        assertEquals("no-store", response.getHeaders().getCacheControl());
    }

    @Test
    void startVerificationDelegates() {
        when(identityOnboardingService.startVerification(user)).thenReturn(Map.of("overallStatus", "IN_PROGRESS"));
        assertEquals("IN_PROGRESS", controller.startVerification().getBody().get("overallStatus"));
    }

    @Test
    void submitSelfieParsesNumberDocumentIdAndBooleanMatch() {
        when(identityOnboardingService.submitSelfie(user, 12L, true)).thenReturn(Map.of("photoStatus", "VERIFIED"));
        ResponseEntity<Map<String, Object>> response = controller.submitSelfie(Map.of(
                "documentId", 12,
                "faceMatchConfirmed", true
        ));
        assertEquals("VERIFIED", response.getBody().get("photoStatus"));
        verify(identityOnboardingService).submitSelfie(user, 12L, true);
    }

    @Test
    void submitSelfieParsesStringDocumentIdAndStringMatch() {
        when(identityOnboardingService.submitSelfie(user, 15L, true)).thenReturn(Map.of("photoStatus", "VERIFIED"));
        controller.submitSelfie(Map.of("documentId", "15", "faceMatchConfirmed", "true"));
        verify(identityOnboardingService).submitSelfie(user, 15L, true);
    }

    @Test
    void submitSelfieNullBodyUsesDefaults() {
        when(identityOnboardingService.submitSelfie(user, null, false)).thenReturn(Map.of("photoStatus", "IN_PROGRESS"));
        controller.submitSelfie(null);
        verify(identityOnboardingService).submitSelfie(user, null, false);
    }

    @Test
    void submitSelfieIgnoresNullFields() {
        when(identityOnboardingService.submitSelfie(user, null, false)).thenReturn(Map.of("photoStatus", "IN_PROGRESS"));
        controller.submitSelfie(Map.of("other", "x"));
        verify(identityOnboardingService).submitSelfie(user, null, false);
    }

    @Test
    void verificationWebhookDelegatesWithoutCurrentUser() {
        when(identityOnboardingService.handleVerificationWebhook(any())).thenReturn(Map.of("ok", true));
        assertEquals(true, controller.verificationWebhook(Map.of("sessionId", "abc")).getBody().get("ok"));
        verify(currentUserService, never()).getCurrentUser();
    }

    @Test
    void backgroundConsentParsesBooleanAccepted() {
        when(identityOnboardingService.acceptBackgroundConsent(user, true, "v1"))
                .thenReturn(Map.of("accepted", true));
        ResponseEntity<Map<String, Object>> response = controller.backgroundConsent(Map.of(
                "accepted", true,
                "documentVersion", "v1"
        ));
        assertEquals(true, response.getBody().get("accepted"));
    }

    @Test
    void backgroundConsentParsesStringAcceptedAndNullVersion() {
        when(identityOnboardingService.acceptBackgroundConsent(user, false, null))
                .thenReturn(Map.of("accepted", false));
        controller.backgroundConsent(Map.of("accepted", "false"));
        verify(identityOnboardingService).acceptBackgroundConsent(user, false, null);
    }

    @Test
    void backgroundConsentNullBody() {
        when(identityOnboardingService.acceptBackgroundConsent(user, null, null))
                .thenReturn(Map.of("accepted", false));
        controller.backgroundConsent(null);
        verify(identityOnboardingService).acceptBackgroundConsent(user, null, null);
    }
}
