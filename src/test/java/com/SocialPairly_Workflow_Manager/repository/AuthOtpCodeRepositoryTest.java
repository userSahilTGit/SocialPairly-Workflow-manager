package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.AuthOtpCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class AuthOtpCodeRepositoryTest {

    @Test
    void findLatestActiveReturnsTheFirstUnconsumedCode() {
        AuthOtpCodeRepository repository = mock(AuthOtpCodeRepository.class, withSettings().defaultAnswer(
                invocation -> {
                    if (invocation.getMethod().isDefault()) {
                        return invocation.callRealMethod();
                    }
                    return null;
                }));
        LocalDateTime now = LocalDateTime.now();
        when(repository.findActive("EMAIL_VERIFY", "ada@example.com")).thenReturn(List.of());
        assertTrue(repository.findLatestActive("EMAIL_VERIFY", "ada@example.com", now).isEmpty());

        AuthOtpCode latest = new AuthOtpCode();
        AuthOtpCode older = new AuthOtpCode();
        when(repository.findActive("EMAIL_VERIFY", "ada@example.com")).thenReturn(List.of(latest, older));
        assertEquals(latest, repository.findLatestActive("EMAIL_VERIFY", "ada@example.com", now).orElseThrow());
    }
}
