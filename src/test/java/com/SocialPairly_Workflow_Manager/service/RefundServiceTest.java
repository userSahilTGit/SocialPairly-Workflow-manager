package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.BankDetailsRepository;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.RefundRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private BankDetailsRepository bankDetailsRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private RefundService refundService;

    private User user;
    private Payment payment;
    private Subscription subscription;
    private Refund refund;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");

        payment = new Payment();
        payment.setId(10L);
        payment.setUser(user);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("usd");
        payment.setStatus("succeeded");

        Plan plan = new Plan();
        plan.setId(5L);

        subscription = new Subscription();
        subscription.setId(20L);
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus("active");

        refund = new Refund();
        refund.setRefundId(99L);
        refund.setUser(user);
        refund.setPayment(payment);
        refund.setStatus(RefundStatus.Initiated);
        refund.setAction(RefundAction.Requested);
        refund.setReason("Not satisfied");
        refund.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getCurrentUserRefundShouldReturnEmptyWhenNoneExists() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findFirstByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        assertTrue(refundService.getCurrentUserRefund().isEmpty());
    }

    @Test
    void getCurrentUserRefundShouldReturnDtoWhenExists() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findFirstByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(refund));

        Optional<RefundDto> result = refundService.getCurrentUserRefund();

        assertTrue(result.isPresent());
        assertEquals(99L, result.get().refundId());
    }

    @Test
    void submitRefundRequestShouldThrowWhenActiveRefundExists() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(1L), anyList())).thenReturn(true);

        RefundRequestDto request = new RefundRequestDto("  reason  ");

        assertThrows(BadRequestException.class, () -> refundService.submitRefundRequest(request));
    }

    @Test
    void submitRefundRequestShouldThrowWhenNoActiveSubscription() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(1L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(1L), any())).thenReturn(List.of());

        RefundRequestDto request = new RefundRequestDto("reason");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> refundService.submitRefundRequest(request));
        assertEquals("No active subscription found to discontinue", ex.getMessage());
    }

    @Test
    void submitRefundRequestShouldThrowWhenNoSuccessfulPayment() {
        payment.setStatus("failed");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(1L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(1L), any())).thenReturn(List.of(subscription));
        when(paymentRepository.findBySubscription_Id(20L)).thenReturn(List.of(payment));

        RefundRequestDto request = new RefundRequestDto("reason");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> refundService.submitRefundRequest(request));
        assertEquals("No successful payment found for your subscription", ex.getMessage());
    }

    @Test
    void submitRefundRequestShouldCreateRefundWhenValid() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(1L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(1L), any())).thenReturn(List.of(subscription));
        when(paymentRepository.findBySubscription_Id(20L)).thenReturn(List.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund r = inv.getArgument(0);
            r.setRefundId(99L);
            return r;
        });

        RefundDto result = refundService.submitRefundRequest(new RefundRequestDto("  reason  "));

        assertEquals(99L, result.refundId());
        assertEquals("reason", result.reason());
        assertEquals("Initiated", result.status());
        assertEquals("Requested", result.action());
    }

    @Test
    void submitSlotShouldThrowWhenActionNotRequestedCall() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));

        SlotRequestDto request = new SlotRequestDto(LocalDateTime.now().plusDays(1));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> refundService.submitSlot(99L, request));
        assertEquals("A consultation slot is not required at this stage", ex.getMessage());
    }

    @Test
    void submitSlotShouldThrowWhenSlotInPast() {
        refund.setAction(RefundAction.Requested_a_Call);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));

        SlotRequestDto request = new SlotRequestDto(LocalDateTime.now().minusHours(1));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> refundService.submitSlot(99L, request));
        assertEquals("Please select a future date and time", ex.getMessage());
    }

    @Test
    void submitSlotShouldSaveSlotWhenValid() {
        refund.setAction(RefundAction.Requested_a_Call);
        LocalDateTime futureSlot = LocalDateTime.now().plusDays(2);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundDto result = refundService.submitSlot(99L, new SlotRequestDto(futureSlot));

        assertEquals("Provided Slot", result.action());
        assertEquals(futureSlot, result.slot());
    }

    @Test
    void submitSlotShouldThrowWhenRefundNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> refundService.submitSlot(99L, new SlotRequestDto(LocalDateTime.now().plusDays(1))));
    }

    @Test
    void submitSlotShouldThrowWhenRefundBelongsToAnotherUser() {
        User other = new User();
        other.setId(2L);
        refund.setUser(other);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));

        assertThrows(BadRequestException.class,
                () -> refundService.submitSlot(99L, new SlotRequestDto(LocalDateTime.now().plusDays(1))));
    }

    @Test
    void submitBankDetailsShouldThrowWhenActionNotApproved() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));

        BankDetailsRequestDto request = createBankDetailsRequest();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> refundService.submitBankDetails(99L, request));
        assertEquals("Bank details are not required at this stage", ex.getMessage());
    }

    @Test
    void submitBankDetailsShouldThrowWhenAlreadySubmitted() {
        refund.setAction(RefundAction.Approved);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));
        when(bankDetailsRepository.findByRefund_RefundId(99L)).thenReturn(Optional.of(new BankDetails()));

        assertThrows(BadRequestException.class,
                () -> refundService.submitBankDetails(99L, createBankDetailsRequest()));
    }

    @Test
    void submitBankDetailsShouldSaveDetailsWhenValid() {
        refund.setAction(RefundAction.Approved);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));
        when(bankDetailsRepository.findByRefund_RefundId(99L)).thenReturn(Optional.empty());
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundDto result = refundService.submitBankDetails(99L, createBankDetailsRequest());

        assertEquals("Provided Bank Details", result.action());
        verify(bankDetailsRepository).save(any(BankDetails.class));
    }

    @Test
    void listRefundsForAdminShouldReturnMappedDtos() {
        when(refundRepository.findAllWithDetails()).thenReturn(List.of(refund));

        List<AdminRefundListDto> result = refundService.listRefundsForAdmin();

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).refundId());
    }

    @Test
    void getRefundDetailForAdminShouldReturnDetailWithBankDetails() {
        BankDetails bankDetails = new BankDetails();
        bankDetails.setBankName("Chase");

        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));
        when(bankDetailsRepository.findByRefund_RefundId(99L)).thenReturn(Optional.of(bankDetails));

        AdminRefundDetailDto result = refundService.getRefundDetailForAdmin(99L);

        assertEquals(99L, result.refundId());
        assertNotNull(result.bankDetails());
        assertEquals("Chase", result.bankDetails().bankName());
    }

    @Test
    void getRefundDetailForAdminShouldThrowWhenNotFound() {
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> refundService.getRefundDetailForAdmin(99L));
    }

    @Test
    void adminRequestCallShouldUpdateRefundStatus() {
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankDetailsRepository.findByRefund_RefundId(99L)).thenReturn(Optional.empty());

        AdminRefundDetailDto result = refundService.adminRequestCall(99L);

        assertEquals("In-Progress", result.status());
        assertEquals("Requested a Call", result.action());
    }

    @Test
    void adminApproveShouldUpdateRefundWhenValid() {
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankDetailsRepository.findByRefund_RefundId(99L)).thenReturn(Optional.empty());

        AdminRefundDetailDto result = refundService.adminApprove(99L);

        assertEquals("Approved", result.action());
    }

    @Test
    void adminApproveShouldThrowWhenActionIsClosed() {
        refund.setAction(RefundAction.Closed);

        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));

        assertThrows(BadRequestException.class, () -> refundService.adminApprove(99L));
    }

    @Test
    void adminCloseShouldRejectRefund() {
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankDetailsRepository.findByRefund_RefundId(99L)).thenReturn(Optional.empty());

        AdminRefundDetailDto result = refundService.adminClose(99L);

        assertEquals("Rejected", result.status());
        assertEquals("Closed", result.action());
    }

    @Test
    void adminCompletePayoutShouldFinalizeRefundAndCancelSubscription() {
        refund.setAction(RefundAction.Provided_Bank_Details);

        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankDetailsRepository.findByRefund_RefundId(99L)).thenReturn(Optional.empty());
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(1L), any()))
                .thenReturn(List.of(subscription));

        AdminRefundDetailDto result = refundService.adminCompletePayout(99L);

        assertEquals("Completed", result.status());
        verify(paymentRepository).save(payment);
        verify(subscriptionRepository).save(subscription);
        assertEquals("cancelled", subscription.getStatus());
        assertNotNull(subscription.getCanceledAt());
        assertEquals(new BigDecimal("95.00"), payment.getAmountRefunded());
    }

    @Test
    void adminCompletePayoutShouldThrowWhenAlreadyCompleted() {
        refund.setStatus(RefundStatus.Completed);
        refund.setAction(RefundAction.Provided_Bank_Details);

        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));

        assertThrows(BadRequestException.class, () -> refundService.adminCompletePayout(99L));
    }

    @Test
    void adminCompletePayoutShouldThrowWhenBankDetailsNotProvided() {
        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));

        assertThrows(BadRequestException.class, () -> refundService.adminCompletePayout(99L));
    }

    @Test
    void adminActionsShouldThrowWhenRefundAlreadyCompleted() {
        refund.setStatus(RefundStatus.Completed);

        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));

        assertThrows(BadRequestException.class, () -> refundService.adminRequestCall(99L));
    }

    @Test
    void adminActionsShouldThrowWhenRefundClosed() {
        refund.setStatus(RefundStatus.Rejected);
        refund.setAction(RefundAction.Closed);

        when(refundRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(refund));

        assertThrows(BadRequestException.class, () -> refundService.adminClose(99L));
    }

    @Test
    void getCurrentUserRefundShouldIncludeNetRefundForCompletedStatus() {
        refund.setStatus(RefundStatus.Completed);
        refund.setAction(RefundAction.Provided_Bank_Details);
        payment.setAmountRefunded(BigDecimal.ZERO);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(refundRepository.findFirstByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(refund));

        Optional<RefundDto> result = refundService.getCurrentUserRefund();

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("95.00"), result.get().refundAmount());
    }

    private BankDetailsRequestDto createBankDetailsRequest() {
        return new BankDetailsRequestDto(
                "John Doe",
                "Chase",
                "123456789",
                "Checking",
                "021000021",
                "123 Main St"
        );
    }
}
