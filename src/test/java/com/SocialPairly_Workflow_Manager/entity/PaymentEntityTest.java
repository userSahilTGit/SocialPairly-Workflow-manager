package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentEntityTest {

    @Test
    void shouldInitializeTimestampsOnCreate() {
        Payment payment = new Payment();
        payment.onCreate();

        assertNotNull(payment.getCreatedAt());
        assertNotNull(payment.getUpdatedAt());
        assertEquals(BigDecimal.ZERO, payment.getAmountRefunded());
        assertEquals("usd", payment.getCurrency());
    }

    @Test
    void shouldUpdateTimestampOnUpdate() {
        Payment payment = new Payment();
        payment.onCreate();
        LocalDateTime originalUpdatedAt = payment.getUpdatedAt();

        payment.onUpdate();

        assertNotNull(payment.getUpdatedAt());
        assertFalse(payment.getUpdatedAt().isBefore(originalUpdatedAt));
    }

    @Test
    void shouldSupportAllGettersAndSetters() {
        User user = new User();
        user.setId(1L);

        Subscription subscription = new Subscription();
        subscription.setId(2L);

        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment();
        payment.setId(10L);
        payment.setUser(user);
        payment.setSubscription(subscription);
        payment.setStripePaymentIntentId("pi_123");
        payment.setStripeChargeId("ch_456");
        payment.setStripeInvoiceId("in_789");
        payment.setAmount(new BigDecimal("99.99"));
        payment.setAmountRefunded(new BigDecimal("5.00"));
        payment.setCurrency("eur");
        payment.setStatus("succeeded");
        payment.setReceiptUrl("https://receipt.example.com");
        payment.setMethod("VISA - 4242");
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        assertEquals(10L, payment.getId());
        assertEquals(user, payment.getUser());
        assertEquals(subscription, payment.getSubscription());
        assertEquals("pi_123", payment.getStripePaymentIntentId());
        assertEquals("ch_456", payment.getStripeChargeId());
        assertEquals("in_789", payment.getStripeInvoiceId());
        assertEquals(new BigDecimal("99.99"), payment.getAmount());
        assertEquals(new BigDecimal("5.00"), payment.getAmountRefunded());
        assertEquals("eur", payment.getCurrency());
        assertEquals("succeeded", payment.getStatus());
        assertEquals("https://receipt.example.com", payment.getReceiptUrl());
        assertEquals("VISA - 4242", payment.getMethod());
        assertEquals(now, payment.getCreatedAt());
        assertEquals(now, payment.getUpdatedAt());
    }
}
