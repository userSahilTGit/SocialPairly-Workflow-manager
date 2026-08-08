package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.SubscriptionDto;
import com.SocialPairly_Workflow_Manager.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    @Test
    void getCurrentSubscriptionShouldReturnDtoWhenActiveSubscriptionExists() {
        SubscriptionDto dto = new SubscriptionDto(
                1L, 5L, "Pro", "MONTHLY", 30,
                new BigDecimal("29.99"), "active",
                LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                false
        );
        when(subscriptionService.getCurrentSubscription()).thenReturn(Optional.of(dto));

        ResponseEntity<?> response = subscriptionController.getCurrentSubscription();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
        assertEquals("Pro", ((SubscriptionDto) response.getBody()).planName());
    }

    @Test
    void getCurrentSubscriptionShouldReturnInactiveWhenNoSubscription() {
        when(subscriptionService.getCurrentSubscription()).thenReturn(Optional.empty());

        ResponseEntity<?> response = subscriptionController.getCurrentSubscription();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, ((Map<?, ?>) response.getBody()).get("active"));
    }
}
