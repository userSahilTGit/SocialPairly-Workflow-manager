package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.service.StripeWebhookService;
import com.stripe.model.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest {

    @Mock
    private StripeWebhookService stripeWebhookService;

    @InjectMocks
    private StripeWebhookController stripeWebhookController;

    @Test
    void handleWebhookShouldConstructEventHandleItAndReturnOk() {
        String payload = "{\"id\":\"evt_123\",\"type\":\"checkout.session.completed\"}";
        String signature = "sig_test";

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(stripeWebhookService.constructEvent(payload, signature)).thenReturn(event);

        ResponseEntity<String> response = stripeWebhookController.handleWebhook(payload, signature);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
        verify(stripeWebhookService).constructEvent(payload, signature);
        verify(stripeWebhookService).handleEvent(event);
    }
}
