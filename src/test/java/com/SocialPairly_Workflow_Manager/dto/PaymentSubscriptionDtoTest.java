package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentSubscriptionDtoTest {

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPhoneNumber("5551234");
        user.setAddress("NYC");
        user.setRole(Role.USER);
        user.setProfileCompleted(true);
        return user;
    }

    private Plan createPlan() {
        Plan plan = new Plan();
        plan.setId(5L);
        plan.setPlanName("Pro");
        plan.setPlanType("MONTHLY");
        plan.setDurationDays(30);
        plan.setAmount(new BigDecimal("29.99"));
        return plan;
    }

    private Payment createPayment(User user) {
        Payment payment = new Payment();
        payment.setId(10L);
        payment.setUser(user);
        payment.setStripeChargeId("ch_abc");
        payment.setStripePaymentIntentId("pi_def");
        payment.setAmount(new BigDecimal("29.99"));
        payment.setCurrency("usd");
        payment.setStatus("SUCCEEDED");
        payment.setReceiptUrl("https://receipt.example.com");
        payment.setCreatedAt(LocalDateTime.of(2026, 8, 1, 12, 0));
        return payment;
    }

    @Test
    void adminPaymentDtoShouldMapFromPaymentWithChargeId() {
        User user = createUser();
        Payment payment = createPayment(user);

        AdminPaymentDto dto = AdminPaymentDto.from(payment);

        assertEquals(10L, dto.id());
        assertEquals(1L, dto.userId());
        assertEquals("John Doe", dto.userName());
        assertEquals("john@example.com", dto.userEmail());
        assertEquals("ch_abc", dto.stripeChargeId());
        assertEquals(new BigDecimal("29.99"), dto.amount());
        assertEquals("usd", dto.currency());
        assertEquals("succeeded", dto.status());
        assertEquals("https://receipt.example.com", dto.receiptUrl());
    }

    @Test
    void adminPaymentDtoShouldUsePaymentIntentWhenChargeIdMissing() {
        User user = createUser();
        user.setFirstName(null);
        user.setLastName(null);
        Payment payment = createPayment(user);
        payment.setStripeChargeId(null);

        AdminPaymentDto dto = AdminPaymentDto.from(payment);

        assertEquals("pi_def", dto.stripeChargeId());
        assertEquals("john@example.com", dto.userName());
    }

    @Test
    void adminPaymentDtoShouldShowRefundedStatusWhenAmountRefunded() {
        User user = createUser();
        Payment payment = createPayment(user);
        payment.setAmountRefunded(new BigDecimal("10.00"));

        AdminPaymentDto dto = AdminPaymentDto.from(payment);

        assertEquals("refunded", dto.status());
    }

    @Test
    void adminPaymentDtoShouldDefaultToPendingWhenStatusNull() {
        User user = createUser();
        Payment payment = createPayment(user);
        payment.setStatus(null);

        AdminPaymentDto dto = AdminPaymentDto.from(payment);

        assertEquals("pending", dto.status());
    }

    @Test
    void adminSubscriptionDtoShouldMapFromSubscription() {
        User user = createUser();
        Plan plan = createPlan();
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 1, 0, 0);

        Subscription subscription = new Subscription();
        subscription.setId(20L);
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setCurrentPeriodStart(start);
        subscription.setCurrentPeriodEnd(end);
        subscription.setStatus("active");

        AdminSubscriptionDto dto = AdminSubscriptionDto.from(subscription);

        assertEquals(20L, dto.id());
        assertEquals(1L, dto.userId());
        assertEquals("John Doe", dto.userName());
        assertEquals(5L, dto.planId());
        assertEquals("Pro", dto.planName());
        assertEquals(start, dto.subscriptionStartDate());
        assertEquals(end, dto.subscriptionEndDate());
        assertEquals("active", dto.status());
    }

    @Test
    void adminSubscriptionDtoShouldUseEmailWhenNameMissing() {
        User user = createUser();
        user.setFirstName(null);
        user.setLastName(null);
        Plan plan = createPlan();

        Subscription subscription = new Subscription();
        subscription.setId(20L);
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus("active");

        AdminSubscriptionDto dto = AdminSubscriptionDto.from(subscription);

        assertEquals("john@example.com", dto.userName());
    }

    @Test
    void subscriptionDtoShouldMapFromSubscription() {
        Plan plan = createPlan();
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 1, 0, 0);

        Subscription subscription = new Subscription();
        subscription.setId(30L);
        subscription.setPlan(plan);
        subscription.setStatus("active");
        subscription.setCurrentPeriodStart(start);
        subscription.setCurrentPeriodEnd(end);
        subscription.setCancelAtPeriodEnd(true);

        SubscriptionDto dto = SubscriptionDto.from(subscription);

        assertEquals(30L, dto.id());
        assertEquals(5L, dto.planId());
        assertEquals("Pro", dto.planName());
        assertEquals("MONTHLY", dto.planType());
        assertEquals(30, dto.durationDays());
        assertEquals(new BigDecimal("29.99"), dto.amount());
        assertEquals("active", dto.status());
        assertEquals(start, dto.currentPeriodStart());
        assertEquals(end, dto.currentPeriodEnd());
        assertTrue(dto.cancelAtPeriodEnd());
    }

    @Test
    void userDtoShouldIncludeSubscriptionDetailsWhenSubscribed() {
        User user = createUser();

        UserDto subscribed = UserDto.from(user, true);
        UserDto unsubscribed = UserDto.from(user, false);
        UserDto defaultDto = UserDto.from(user);

        assertEquals("Subscribed", subscribed.subscriptionDetails());
        assertEquals("Unsubscribed", unsubscribed.subscriptionDetails());
        assertEquals("", defaultDto.subscriptionDetails());
    }

    @Test
    void receiptDtoShouldExposeAllFields() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 8, 1, 12, 0);
        ReceiptDto dto = new ReceiptDto(
                "RCPT-001",
                "Pro",
                new BigDecimal("29.99"),
                "usd",
                paidAt,
                "john@example.com",
                "https://receipt.example.com",
                "SocialPairly",
                "support@example.com",
                "VISA - 4242"
        );

        assertEquals("RCPT-001", dto.receiptNumber());
        assertEquals("Pro", dto.planName());
        assertEquals(new BigDecimal("29.99"), dto.amount());
        assertEquals("usd", dto.currency());
        assertEquals(paidAt, dto.paidAt());
        assertEquals("john@example.com", dto.customerEmail());
        assertEquals("https://receipt.example.com", dto.receiptUrl());
        assertEquals("SocialPairly", dto.merchantName());
        assertEquals("support@example.com", dto.supportEmail());
        assertEquals("VISA - 4242", dto.paymentMethod());
    }

    @Test
    void refundDtosShouldMapFromRefundEntity() {
        User user = createUser();
        Payment payment = createPayment(user);
        payment.setAmountRefunded(new BigDecimal("28.49"));

        Refund refund = new Refund();
        refund.setRefundId(99L);
        refund.setUser(user);
        refund.setPayment(payment);
        refund.setStatus(RefundStatus.Completed);
        refund.setAction(RefundAction.Provided_Bank_Details);
        refund.setReason("Service issue");
        refund.setCreatedAt(LocalDateTime.of(2026, 8, 5, 9, 0));

        RefundDto dto = RefundDto.from(refund);
        RefundDto withNet = RefundDto.from(refund, new BigDecimal("28.49"));

        assertEquals(99L, dto.refundId());
        assertEquals("REF-99", dto.formattedRefundId());
        assertEquals("Completed", dto.status());
        assertEquals("Provided Bank Details", dto.action());
        assertEquals("Service issue", dto.reason());
        assertEquals(new BigDecimal("29.99"), dto.amount());
        assertEquals("usd", dto.currency());
        assertEquals(new BigDecimal("28.49"), dto.refundAmount());
        assertEquals(new BigDecimal("28.49"), withNet.refundAmount());
    }

    @Test
    void refundDtoShouldReturnNullRefundAmountWhenNotRefunded() {
        User user = createUser();
        Payment payment = createPayment(user);

        Refund refund = new Refund();
        refund.setRefundId(1L);
        refund.setUser(user);
        refund.setPayment(payment);
        refund.setStatus(RefundStatus.Initiated);
        refund.setAction(RefundAction.Requested);
        refund.setReason("Test");

        RefundDto dto = RefundDto.from(refund);

        assertNull(dto.refundAmount());
    }

    @Test
    void adminRefundListDtoShouldMapFromRefund() {
        User user = createUser();
        Payment payment = createPayment(user);

        Refund refund = new Refund();
        refund.setRefundId(5L);
        refund.setUser(user);
        refund.setPayment(payment);
        refund.setStatus(RefundStatus.Initiated);
        refund.setAction(RefundAction.Requested);
        refund.setCreatedAt(LocalDateTime.of(2026, 8, 5, 9, 0));

        AdminRefundListDto dto = AdminRefundListDto.from(refund);

        assertEquals(5L, dto.refundId());
        assertEquals("REF-5", dto.formattedRefundId());
        assertEquals(new BigDecimal("29.99"), dto.amount());
        assertEquals("Initiated", dto.status());
        assertEquals("Requested", dto.action());
    }

    @Test
    void adminRefundListDtoShouldUseDashWhenStatusOrActionNull() {
        User user = createUser();
        Payment payment = createPayment(user);

        Refund refund = new Refund();
        refund.setRefundId(6L);
        refund.setUser(user);
        refund.setPayment(payment);

        AdminRefundListDto dto = AdminRefundListDto.from(refund);

        assertEquals("—", dto.status());
        assertEquals("—", dto.action());
    }

    @Test
    void adminRefundDetailDtoShouldMapWithAndWithoutBankDetails() {
        User user = createUser();
        Payment payment = createPayment(user);
        LocalDateTime slot = LocalDateTime.of(2026, 8, 10, 14, 0);

        Refund refund = new Refund();
        refund.setRefundId(8L);
        refund.setUser(user);
        refund.setPayment(payment);
        refund.setStatus(RefundStatus.In_Progress);
        refund.setAction(RefundAction.Provided_Slot);
        refund.setReason("Unhappy");
        refund.setSlot(slot);
        refund.setCreatedAt(LocalDateTime.of(2026, 8, 5, 9, 0));

        BankDetails bankDetails = new BankDetails();
        bankDetails.setAccountHolderName("John Doe");
        bankDetails.setBankName("Chase");
        bankDetails.setAccountNumber("123456");
        bankDetails.setAccountType("Checking");
        bankDetails.setAbaRoutingNumber("021000021");
        bankDetails.setRecipientsAddress("123 Main St");

        AdminRefundDetailDto withBank = AdminRefundDetailDto.from(refund, bankDetails);
        AdminRefundDetailDto withoutBank = AdminRefundDetailDto.from(refund, null);

        assertEquals(8L, withBank.refundId());
        assertEquals("John Doe", withBank.userName());
        assertEquals("john@example.com", withBank.userEmail());
        assertEquals(new BigDecimal("29.99"), withBank.claimAmount());
        assertEquals("In-Progress", withBank.status());
        assertEquals("Provided Slot", withBank.action());
        assertEquals(slot, withBank.slot());
        assertNotNull(withBank.bankDetails());
        assertEquals("Chase", withBank.bankDetails().bankName());
        assertNull(withoutBank.bankDetails());
    }

    @Test
    void bankDetailsDtoShouldMapFromEntity() {
        BankDetails details = new BankDetails();
        details.setAccountHolderName("Jane");
        details.setBankName("BoA");
        details.setAccountNumber("999");
        details.setAccountType("Savings");
        details.setAbaRoutingNumber("111");
        details.setRecipientsAddress("456 Oak");

        BankDetailsDto dto = BankDetailsDto.from(details);

        assertEquals("Jane", dto.accountHolderName());
        assertEquals("BoA", dto.bankName());
        assertEquals("999", dto.accountNumber());
        assertEquals("Savings", dto.accountType());
        assertEquals("111", dto.abaRoutingNumber());
        assertEquals("456 Oak", dto.recipientsAddress());
    }
}
