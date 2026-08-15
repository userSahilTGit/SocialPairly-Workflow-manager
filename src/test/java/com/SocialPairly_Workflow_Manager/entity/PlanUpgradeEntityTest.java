package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PlanUpgradeEntityTest {

    @Test
    void planUpgradeRequestShouldInitializeDefaultsOnCreate() {
        PlanUpgradeRequest request = new PlanUpgradeRequest();
        request.setStatus(null);
        request.setAction(null);
        request.setExtraToken(null);
        request.setExtraAmount(null);

        request.onCreate();

        assertNotNull(request.getCreatedAt());
        assertNotNull(request.getUpdatedAt());
        assertEquals(PlanUpgradeStatus.Started, request.getStatus());
        assertEquals(PlanUpgradeAction.Requested, request.getAction());
        assertEquals(0, request.getExtraToken());
        assertEquals(BigDecimal.ZERO, request.getExtraAmount());
    }

    @Test
    void planUpgradeRequestShouldKeepExistingValuesOnCreate() {
        PlanUpgradeRequest request = new PlanUpgradeRequest();
        request.setStatus(PlanUpgradeStatus.InProgress);
        request.setAction(PlanUpgradeAction.Approved);
        request.setExtraToken(25);
        request.setExtraAmount(new BigDecimal("12.50"));

        request.onCreate();

        assertEquals(PlanUpgradeStatus.InProgress, request.getStatus());
        assertEquals(PlanUpgradeAction.Approved, request.getAction());
        assertEquals(25, request.getExtraToken());
        assertEquals(new BigDecimal("12.50"), request.getExtraAmount());
    }

    @Test
    void planUpgradeRequestShouldUpdateTimestampOnUpdate() {
        PlanUpgradeRequest request = new PlanUpgradeRequest();
        request.onCreate();
        LocalDateTime created = request.getCreatedAt();

        request.onUpdate();

        assertEquals(created, request.getCreatedAt());
        assertNotNull(request.getUpdatedAt());
    }

    @Test
    void planUpgradeRequestShouldFormatUpgradeId() {
        PlanUpgradeRequest request = new PlanUpgradeRequest();
        request.setId(7L);
        assertEquals("UPG-7", request.getFormattedUpgradeId());
    }

    @Test
    void planUpgradeRequestShouldSupportAllGettersAndSetters() {
        User user = new User();
        user.setId(22L);

        Payment payment = new Payment();
        payment.setId(55L);

        LocalDateTime created = LocalDateTime.of(2026, 8, 14, 10, 0);
        LocalDateTime updated = LocalDateTime.of(2026, 8, 14, 11, 0);

        PlanUpgradeRequest request = new PlanUpgradeRequest();
        request.setId(1L);
        request.setUser(user);
        request.setCurrentPlan("Welcome Pass");
        request.setUpgradePlan("Basic Monthly");
        request.setReason("Need more tokens");
        request.setStatus(PlanUpgradeStatus.InProgress);
        request.setAction(PlanUpgradeAction.Approved);
        request.setExtraToken(40);
        request.setExtraAmount(new BigDecimal("20.00"));
        request.setPayment(payment);
        request.setCreatedAt(created);
        request.setUpdatedAt(updated);

        assertEquals(1L, request.getId());
        assertEquals(user, request.getUser());
        assertEquals("Welcome Pass", request.getCurrentPlan());
        assertEquals("Basic Monthly", request.getUpgradePlan());
        assertEquals("Need more tokens", request.getReason());
        assertEquals(PlanUpgradeStatus.InProgress, request.getStatus());
        assertEquals(PlanUpgradeAction.Approved, request.getAction());
        assertEquals(40, request.getExtraToken());
        assertEquals(new BigDecimal("20.00"), request.getExtraAmount());
        assertEquals(payment, request.getPayment());
        assertEquals(created, request.getCreatedAt());
        assertEquals(updated, request.getUpdatedAt());
    }

    @Test
    void planUpgradeStatusShouldConvertDisplayValues() {
        assertEquals("Started", PlanUpgradeStatus.Started.getDisplayValue());
        assertEquals("InProgress", PlanUpgradeStatus.InProgress.getDisplayValue());
        assertEquals("Completed", PlanUpgradeStatus.Completed.getDisplayValue());
        assertEquals("Rejected", PlanUpgradeStatus.Rejected.getDisplayValue());

        assertEquals(PlanUpgradeStatus.Started, PlanUpgradeStatus.fromDisplayValue("Started"));
        assertEquals(PlanUpgradeStatus.InProgress, PlanUpgradeStatus.fromDisplayValue("InProgress"));
        assertEquals(PlanUpgradeStatus.Completed, PlanUpgradeStatus.fromDisplayValue("Completed"));
        assertEquals(PlanUpgradeStatus.Rejected, PlanUpgradeStatus.fromDisplayValue("Rejected"));
        assertNull(PlanUpgradeStatus.fromDisplayValue(null));
    }

    @Test
    void planUpgradeActionShouldConvertDisplayValues() {
        assertEquals("Requested", PlanUpgradeAction.Requested.getDisplayValue());
        assertEquals("Approved", PlanUpgradeAction.Approved.getDisplayValue());
        assertEquals("Closed", PlanUpgradeAction.Closed.getDisplayValue());

        assertEquals(PlanUpgradeAction.Requested, PlanUpgradeAction.fromDisplayValue("Requested"));
        assertEquals(PlanUpgradeAction.Approved, PlanUpgradeAction.fromDisplayValue("Approved"));
        assertEquals(PlanUpgradeAction.Closed, PlanUpgradeAction.fromDisplayValue("Closed"));
        assertNull(PlanUpgradeAction.fromDisplayValue(null));
    }

    @Test
    void planUpgradeStatusConverterShouldRoundTripValues() {
        PlanUpgradeStatusConverter converter = new PlanUpgradeStatusConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertEquals("InProgress", converter.convertToDatabaseColumn(PlanUpgradeStatus.InProgress));
        assertEquals(PlanUpgradeStatus.Started, converter.convertToEntityAttribute("Started"));
        assertEquals(PlanUpgradeStatus.Rejected, converter.convertToEntityAttribute("Rejected"));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void planUpgradeActionConverterShouldRoundTripValues() {
        PlanUpgradeActionConverter converter = new PlanUpgradeActionConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertEquals("Approved", converter.convertToDatabaseColumn(PlanUpgradeAction.Approved));
        assertEquals(PlanUpgradeAction.Requested, converter.convertToEntityAttribute("Requested"));
        assertEquals(PlanUpgradeAction.Closed, converter.convertToEntityAttribute("Closed"));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
