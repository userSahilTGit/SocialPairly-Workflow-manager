package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final String PAYLOAD = "{\"id\":\"evt_123\"}";
    private static final String SIGNATURE = "sig_test";

    @Mock
    private CheckoutFulfillmentService checkoutFulfillmentService;

    @InjectMocks
    private StripeWebhookService stripeWebhookService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stripeWebhookService, "webhookSecret", WEBHOOK_SECRET);
    }

    @Test
    void constructEventShouldThrowWhenWebhookSecretBlank() {
        ReflectionTestUtils.setField(stripeWebhookService, "webhookSecret", "  ");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> stripeWebhookService.constructEvent(PAYLOAD, SIGNATURE));

        assertEquals("Stripe webhook secret is not configured", ex.getMessage());
    }

    @Test
    void constructEventShouldThrowWhenWebhookSecretNull() {
        ReflectionTestUtils.setField(stripeWebhookService, "webhookSecret", null);

        assertThrows(BadRequestException.class,
                () -> stripeWebhookService.constructEvent(PAYLOAD, SIGNATURE));
    }

    @Test
    void constructEventShouldReturnEventWhenSignatureValid() {
        Event event = mock(Event.class);

        try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
            webhookStatic.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, WEBHOOK_SECRET))
                    .thenReturn(event);

            Event result = stripeWebhookService.constructEvent(PAYLOAD, SIGNATURE);

            assertSame(event, result);
        }
    }

    @Test
    void constructEventShouldThrowWhenSignatureInvalid() {
        try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
            webhookStatic.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, WEBHOOK_SECRET))
                    .thenThrow(new SignatureVerificationException("bad signature", SIGNATURE));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> stripeWebhookService.constructEvent(PAYLOAD, SIGNATURE));

            assertEquals("Invalid Stripe webhook signature", ex.getMessage());
        }
    }

    @Test
    void handleEventShouldFulfillCheckoutSessionCompleted() {
        Session session = mock(Session.class);
        Event event = mockCheckoutEvent("checkout.session.completed", session);

        stripeWebhookService.handleEvent(event);

        verify(checkoutFulfillmentService).fulfillPaidSession(session, null);
    }

    @Test
    void handleEventShouldSkipFulfillmentWhenSessionCannotBeDeserialized() {
        StripeObject wrongObject = mock(StripeObject.class);
        Event event = mockCheckoutEvent("checkout.session.completed", wrongObject);
        when(event.getId()).thenReturn("evt_bad_session");

        stripeWebhookService.handleEvent(event);

        verify(checkoutFulfillmentService, never()).fulfillPaidSession(any(), any());
    }

    @Test
    void handleEventShouldRecordFailedPaymentIntent() {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        Event event = mockCheckoutEvent("payment_intent.payment_failed", paymentIntent);

        stripeWebhookService.handleEvent(event);

        verify(checkoutFulfillmentService).recordFailedPaymentIntent(paymentIntent);
    }

    @Test
    void handleEventShouldSkipWhenPaymentIntentCannotBeDeserialized() {
        StripeObject wrongObject = mock(StripeObject.class);
        Event event = mockCheckoutEvent("payment_intent.payment_failed", wrongObject);
        when(event.getId()).thenReturn("evt_bad_pi");

        stripeWebhookService.handleEvent(event);

        verify(checkoutFulfillmentService, never()).recordFailedPaymentIntent(any());
    }

    @Test
    void handleEventShouldIgnoreUnhandledEventTypes() {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.created");

        stripeWebhookService.handleEvent(event);

        verifyNoInteractions(checkoutFulfillmentService);
    }

    @Test
    void handleEventShouldDeserializeViaUnsafeWhenObjectNotPresent() throws Exception {
        Session session = mock(Session.class);
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.empty());
        when(deserializer.deserializeUnsafe()).thenReturn(session);

        stripeWebhookService.handleEvent(event);

        verify(checkoutFulfillmentService).fulfillPaidSession(session, null);
    }

    @Test
    void handleEventShouldHandleDeserializeUnsafeFailure() throws Exception {
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getId()).thenReturn("evt_deser_fail");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.empty());
        when(deserializer.deserializeUnsafe()).thenThrow(new RuntimeException("deserialize failed"));

        stripeWebhookService.handleEvent(event);

        verify(checkoutFulfillmentService, never()).fulfillPaidSession(any(), any());
    }

    private Event mockCheckoutEvent(String type, StripeObject stripeObject) {
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(event.getType()).thenReturn(type);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeObject));

        return event;
    }
}
