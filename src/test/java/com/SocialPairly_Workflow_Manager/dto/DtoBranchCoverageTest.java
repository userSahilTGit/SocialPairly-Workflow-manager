package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.constants.OnboardingSteps;
import com.SocialPairly_Workflow_Manager.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DtoBranchCoverageTest {

    @Test
    void adminRefundDetailCoversNameEmailAndNullEnums() {
        User user = new User();
        user.setEmail("u@test.com");
        user.setFirstName(null);
        user.setLastName(null);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("10.00"));

        Refund refund = new Refund();
        refund.setRefundId(1L);
        refund.setUser(user);
        refund.setPayment(payment);
        refund.setStatus(null);
        refund.setAction(null);
        refund.setReason("r");
        refund.setCreatedAt(LocalDateTime.now());

        AdminRefundDetailDto emptyName = AdminRefundDetailDto.from(refund, null);
        assertEquals("u@test.com", emptyName.userName());
        assertNull(emptyName.status());
        assertNull(emptyName.action());
        assertNull(emptyName.bankDetails());

        user.setFirstName("Ada");
        user.setLastName(null);
        refund.setStatus(RefundStatus.Initiated);
        refund.setAction(RefundAction.Requested);
        BankDetails bank = new BankDetails();
        bank.setAccountHolderName("Ada");
        bank.setAccountNumber("123");
        bank.setAbaRoutingNumber("021000021");
        bank.setBankName("Bank");
        bank.setAccountType("CHECKING");
        bank.setRecipientsAddress("Addr");
        AdminRefundDetailDto named = AdminRefundDetailDto.from(refund, bank);
        assertEquals("Ada", named.userName());
        assertNotNull(named.status());
        assertNotNull(named.bankDetails());

        user.setFirstName(null);
        user.setLastName("Lovelace");
        assertEquals("Lovelace", AdminRefundDetailDto.from(refund, null).userName());
    }

    @Test
    void userDtoCoversPreferredBlankAndProfileNull() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Ada");
        user.setPreferredName("  ");
        user.setEmail("a@b.com");
        user.setRole(Role.USER);
        user.setUserTokens(0);

        UserDto blankPreferred = UserDto.from(user, (UserProfile) null);
        assertEquals("Ada", blankPreferred.displayName());
        assertNull(blankPreferred.onboardingStep());
        assertFalse(blankPreferred.identityPage1Complete());

        user.setPreferredName("Addie");
        UserProfile profile = new UserProfile();
        profile.setOnboardingStep(OnboardingSteps.IDENTITY_COMPLETED);
        profile.setIdentityPage1CompletedAt(LocalDateTime.now());
        UserDto withProfile = UserDto.from(user, profile);
        assertEquals("Addie", withProfile.displayName());
        assertTrue(withProfile.identityPage1Complete());

        assertEquals("Subscribed", UserDto.from(user, true).subscriptionDetails());
        assertEquals("Unsubscribed", UserDto.from(user, false).subscriptionDetails());
    }

    @Test
    void refundDtoCoversNullEnumsAndRefundedAmountBranches() {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("50.00"));
        payment.setCurrency("usd");
        payment.setAmountRefunded(null);

        Refund refund = new Refund();
        refund.setRefundId(2L);
        refund.setPayment(payment);
        refund.setStatus(null);
        refund.setAction(null);

        RefundDto dto = RefundDto.from(refund);
        assertNull(dto.status());
        assertNull(dto.action());
        assertNull(dto.refundAmount());

        payment.setAmountRefunded(BigDecimal.ZERO);
        assertNull(RefundDto.from(refund).refundAmount());

        payment.setAmountRefunded(new BigDecimal("5.00"));
        assertEquals(new BigDecimal("5.00"), RefundDto.from(refund).refundAmount());

        RefundDto overridden = RefundDto.from(refund, new BigDecimal("4.00"));
        assertEquals(new BigDecimal("4.00"), overridden.refundAmount());
    }
}
