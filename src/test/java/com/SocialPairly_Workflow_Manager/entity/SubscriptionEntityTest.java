package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionEntityTest {

    @Test
    void shouldInitializeTimestampsOnCreate() {
        Subscription subscription = new Subscription();
        subscription.onCreate();

        assertNotNull(subscription.getCreatedAt());
        assertNotNull(subscription.getUpdatedAt());
        assertFalse(subscription.isCancelAtPeriodEnd());
    }

    @Test
    void shouldUpdateTimestampOnUpdate() {
        Subscription subscription = new Subscription();
        subscription.onCreate();
        LocalDateTime originalUpdatedAt = subscription.getUpdatedAt();

        subscription.onUpdate();

        assertNotNull(subscription.getUpdatedAt());
        assertFalse(subscription.getUpdatedAt().isBefore(originalUpdatedAt));
    }

    @Test
    void shouldSupportAllGettersAndSetters() {
        User user = new User();
        user.setId(1L);

        Plan plan = new Plan();
        plan.setId(5L);

        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime canceled = LocalDateTime.of(2026, 1, 15, 12, 0);

        Subscription subscription = new Subscription();
        subscription.setId(100L);
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStripeCustomerId("cus_abc");
        subscription.setStripeSubscriptionId("sub_xyz");
        subscription.setStatus("active");
        subscription.setCurrentPeriodStart(start);
        subscription.setCurrentPeriodEnd(end);
        subscription.setCancelAtPeriodEnd(true);
        subscription.setCanceledAt(canceled);
        subscription.setCreatedAt(start);
        subscription.setUpdatedAt(end);

        assertEquals(100L, subscription.getId());
        assertEquals(user, subscription.getUser());
        assertEquals(plan, subscription.getPlan());
        assertEquals("cus_abc", subscription.getStripeCustomerId());
        assertEquals("sub_xyz", subscription.getStripeSubscriptionId());
        assertEquals("active", subscription.getStatus());
        assertEquals(start, subscription.getCurrentPeriodStart());
        assertEquals(end, subscription.getCurrentPeriodEnd());
        assertTrue(subscription.isCancelAtPeriodEnd());
        assertEquals(canceled, subscription.getCanceledAt());
        assertEquals(start, subscription.getCreatedAt());
        assertEquals(end, subscription.getUpdatedAt());
    }
}
