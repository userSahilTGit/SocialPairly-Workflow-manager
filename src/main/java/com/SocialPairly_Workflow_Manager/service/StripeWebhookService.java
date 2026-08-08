package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final CheckoutFulfillmentService checkoutFulfillmentService;

    @Value("${stripe.webhookSecret:}")
    private String webhookSecret;

    public StripeWebhookService(CheckoutFulfillmentService checkoutFulfillmentService) {
        this.checkoutFulfillmentService = checkoutFulfillmentService;
    }

    public Event constructEvent(String payload, String sigHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new BadRequestException("Stripe webhook secret is not configured");
        }
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.warn("Stripe webhook signature verification failed", e);
            throw new BadRequestException("Invalid Stripe webhook signature");
        }
    }

    @Transactional
    public void handleEvent(Event event) {
        switch (event.getType()) {
            case "checkout.session.completed" -> {
                Session session = deserializeSession(event);
                if (session != null) {
                    checkoutFulfillmentService.fulfillPaidSession(session, null);
                }
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent paymentIntent = deserializePaymentIntent(event);
                if (paymentIntent != null) {
                    checkoutFulfillmentService.recordFailedPaymentIntent(paymentIntent);
                }
            }
            default -> log.debug("Unhandled Stripe webhook event type={}", event.getType());
        }
    }

    private Session deserializeSession(Event event) {
        StripeObject stripeObject = deserializeObject(event);
        if (stripeObject instanceof Session session) {
            return session;
        }
        log.warn("Unable to deserialize checkout session from event {}", event.getId());
        return null;
    }

    private PaymentIntent deserializePaymentIntent(Event event) {
        StripeObject stripeObject = deserializeObject(event);
        if (stripeObject instanceof PaymentIntent paymentIntent) {
            return paymentIntent;
        }
        log.warn("Unable to deserialize payment intent from event {}", event.getId());
        return null;
    }

    private StripeObject deserializeObject(Event event) {
        var deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return deserializer.getObject().get();
        }
        try {
            return deserializer.deserializeUnsafe();
        } catch (Exception e) {
            log.error("Failed to deserialize Stripe event object for type={}", event.getType(), e);
            return null;
        }
    }
}
