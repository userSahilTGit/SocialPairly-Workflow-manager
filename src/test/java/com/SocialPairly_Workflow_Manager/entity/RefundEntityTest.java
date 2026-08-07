package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RefundEntityTest {

    @Test
    void refundShouldInitializeCreatedAtOnCreate() {
        Refund refund = new Refund();
        refund.onCreate();

        assertNotNull(refund.getCreatedAt());
    }

    @Test
    void refundShouldFormatRefundId() {
        Refund refund = new Refund();
        refund.setRefundId(42L);

        assertEquals("REF-42", refund.getFormattedRefundId());
    }

    @Test
    void refundShouldSupportAllGettersAndSetters() {
        User user = new User();
        user.setId(1L);

        Payment payment = new Payment();
        payment.setId(2L);
        payment.setAmount(new BigDecimal("100.00"));

        LocalDateTime slot = LocalDateTime.of(2026, 8, 10, 14, 0);
        LocalDateTime created = LocalDateTime.of(2026, 8, 7, 10, 0);

        Refund refund = new Refund();
        refund.setRefundId(7L);
        refund.setPayment(payment);
        refund.setUser(user);
        refund.setStatus(RefundStatus.In_Progress);
        refund.setReason("Not satisfied");
        refund.setAction(RefundAction.Requested_a_Call);
        refund.setSlot(slot);
        refund.setCreatedAt(created);

        assertEquals(7L, refund.getRefundId());
        assertEquals(payment, refund.getPayment());
        assertEquals(user, refund.getUser());
        assertEquals(RefundStatus.In_Progress, refund.getStatus());
        assertEquals("Not satisfied", refund.getReason());
        assertEquals(RefundAction.Requested_a_Call, refund.getAction());
        assertEquals(slot, refund.getSlot());
        assertEquals(created, refund.getCreatedAt());
    }

    @Test
    void bankDetailsShouldInitializeCreatedAtOnCreate() {
        BankDetails details = new BankDetails();
        details.onCreate();

        assertNotNull(details.getCreatedAt());
    }

    @Test
    void bankDetailsShouldSupportAllGettersAndSetters() {
        Refund refund = new Refund();
        refund.setRefundId(3L);

        LocalDateTime created = LocalDateTime.now();

        BankDetails details = new BankDetails();
        details.setId(1L);
        details.setUserId(5L);
        details.setRefund(refund);
        details.setAccountNumber("123456789");
        details.setAccountType("Checking");
        details.setAccountHolderName("Jane Doe");
        details.setAbaRoutingNumber("021000021");
        details.setBankName("Chase");
        details.setRecipientsAddress("123 Main St");
        details.setCreatedAt(created);

        assertEquals(1L, details.getId());
        assertEquals(5L, details.getUserId());
        assertEquals(refund, details.getRefund());
        assertEquals("123456789", details.getAccountNumber());
        assertEquals("Checking", details.getAccountType());
        assertEquals("Jane Doe", details.getAccountHolderName());
        assertEquals("021000021", details.getAbaRoutingNumber());
        assertEquals("Chase", details.getBankName());
        assertEquals("123 Main St", details.getRecipientsAddress());
        assertEquals(created, details.getCreatedAt());
    }

    @Test
    void refundStatusShouldConvertDisplayValues() {
        assertEquals("In-Progress", RefundStatus.In_Progress.getDisplayValue());
        assertEquals("Initiated", RefundStatus.Initiated.getDisplayValue());
        assertEquals(RefundStatus.In_Progress, RefundStatus.fromDisplayValue("In-Progress"));
        assertEquals(RefundStatus.Completed, RefundStatus.fromDisplayValue("Completed"));
        assertNull(RefundStatus.fromDisplayValue(null));
    }

    @Test
    void refundActionShouldConvertDisplayValues() {
        assertEquals("Requested a Call", RefundAction.Requested_a_Call.getDisplayValue());
        assertEquals("Provided Slot", RefundAction.Provided_Slot.getDisplayValue());
        assertEquals("Provided Bank Details", RefundAction.Provided_Bank_Details.getDisplayValue());
        assertEquals("Requested", RefundAction.Requested.getDisplayValue());

        assertEquals(RefundAction.Requested_a_Call, RefundAction.fromDisplayValue("Requested a Call"));
        assertEquals(RefundAction.Provided_Slot, RefundAction.fromDisplayValue("Provided Slot"));
        assertEquals(RefundAction.Provided_Bank_Details, RefundAction.fromDisplayValue("Provided Bank Details"));
        assertEquals(RefundAction.Closed, RefundAction.fromDisplayValue("Closed"));
        assertNull(RefundAction.fromDisplayValue(null));
    }

    @Test
    void refundStatusConverterShouldRoundTripValues() {
        RefundStatusConverter converter = new RefundStatusConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertEquals("In-Progress", converter.convertToDatabaseColumn(RefundStatus.In_Progress));
        assertEquals(RefundStatus.In_Progress, converter.convertToEntityAttribute("In-Progress"));
        assertEquals(RefundStatus.Rejected, converter.convertToEntityAttribute("Rejected"));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void refundActionConverterShouldRoundTripValues() {
        RefundActionConverter converter = new RefundActionConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertEquals("Requested a Call", converter.convertToDatabaseColumn(RefundAction.Requested_a_Call));
        assertEquals(RefundAction.Provided_Bank_Details, converter.convertToEntityAttribute("Provided Bank Details"));
        assertEquals(RefundAction.Approved, converter.convertToEntityAttribute("Approved"));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
