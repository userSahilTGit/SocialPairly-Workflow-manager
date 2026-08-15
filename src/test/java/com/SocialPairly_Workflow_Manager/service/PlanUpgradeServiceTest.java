package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import com.SocialPairly_Workflow_Manager.repository.PlanUpgradeRequestRepository;
import com.SocialPairly_Workflow_Manager.repository.RefundRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanUpgradeServiceTest {

    @Mock private PlanUpgradeRequestRepository upgradeRequestRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PlanRepository planRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private PlanUpgradeService planUpgradeService;

    private MockedStatic<PaymentIntent> paymentIntentStatic;
    private MockedStatic<Charge> chargeStatic;
    private MockedStatic<PaymentMethod> paymentMethodStatic;

    private User user;
    private Plan currentPlan;
    private Plan upgradePlan;
    private Subscription subscription;
    private PlanUpgradeRequest upgradeRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(planUpgradeService, "secretKey", "sk_test");
        ReflectionTestUtils.setField(planUpgradeService, "merchantName", "SocialPairly");
        ReflectionTestUtils.setField(planUpgradeService, "supportEmail", "support@test.com");

        user = new User();
        user.setId(22L);
        user.setFirstName("Sahil");
        user.setLastName("T");
        user.setEmail("sahil@example.com");
        user.setUserTokens(100);

        currentPlan = new Plan();
        currentPlan.setId(1L);
        currentPlan.setPlanName("Welcome Pass");
        currentPlan.setTokensIncluded("80");
        currentPlan.setAmount(new BigDecimal("14.99"));
        currentPlan.setDurationDays(7);
        currentPlan.setActive(true);

        upgradePlan = new Plan();
        upgradePlan.setId(2L);
        upgradePlan.setPlanName("Basic Monthly");
        upgradePlan.setTokensIncluded("200");
        upgradePlan.setAmount(new BigDecimal("50.00"));
        upgradePlan.setDurationDays(30);
        upgradePlan.setActive(true);
        upgradePlan.setPlanType("MONTHLY");

        subscription = new Subscription();
        subscription.setId(10L);
        subscription.setUser(user);
        subscription.setPlan(currentPlan);
        subscription.setStatus("active");
        subscription.setStripeSubscriptionId("cs_old");

        upgradeRequest = new PlanUpgradeRequest();
        upgradeRequest.setId(1L);
        upgradeRequest.setUser(user);
        upgradeRequest.setCurrentPlan("Welcome Pass");
        upgradeRequest.setUpgradePlan("Basic Monthly");
        upgradeRequest.setReason("Need more tokens");
        upgradeRequest.setStatus(PlanUpgradeStatus.Started);
        upgradeRequest.setAction(PlanUpgradeAction.Requested);
        upgradeRequest.setExtraToken(0);
        upgradeRequest.setExtraAmount(BigDecimal.ZERO);
        upgradeRequest.setCreatedAt(LocalDateTime.now());
        upgradeRequest.setUpdatedAt(LocalDateTime.now());

        paymentIntentStatic = mockStatic(PaymentIntent.class);
        chargeStatic = mockStatic(Charge.class);
        paymentMethodStatic = mockStatic(PaymentMethod.class);
    }

    @AfterEach
    void tearDown() {
        if (paymentIntentStatic != null) paymentIntentStatic.close();
        if (chargeStatic != null) chargeStatic.close();
        if (paymentMethodStatic != null) paymentMethodStatic.close();
    }

    @Test
    void getCurrentUserUpgradeShouldReturnEmptyWhenNone() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.findFirstByUser_IdOrderByCreatedAtDesc(22L)).thenReturn(Optional.empty());

        assertTrue(planUpgradeService.getCurrentUserUpgrade().isEmpty());
    }

    @Test
    void getCurrentUserUpgradeShouldReturnDto() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.findFirstByUser_IdOrderByCreatedAtDesc(22L))
                .thenReturn(Optional.of(upgradeRequest));

        Optional<PlanUpgradeDto> result = planUpgradeService.getCurrentUserUpgrade();

        assertTrue(result.isPresent());
        assertEquals("UPG-1", result.get().formattedUpgradeId());
        assertEquals(100, result.get().userTokens());
    }

    @Test
    void getEligibleUpgradePlansShouldFilterByTokensAndCurrentPlan() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));

        Plan lower = new Plan();
        lower.setId(3L);
        lower.setPlanName("Tiny");
        lower.setTokensIncluded("50");
        lower.setActive(true);
        lower.setAmount(BigDecimal.ONE);
        lower.setDurationDays(3);
        lower.setPlanType("WEEKLY");

        Plan sameName = new Plan();
        sameName.setId(4L);
        sameName.setPlanName("Welcome Pass");
        sameName.setTokensIncluded("300");
        sameName.setActive(true);
        sameName.setAmount(BigDecimal.TEN);
        sameName.setDurationDays(7);
        sameName.setPlanType("WEEKLY");

        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc())
                .thenReturn(List.of(currentPlan, upgradePlan, lower, sameName));

        List<PlanDto> eligible = planUpgradeService.getEligibleUpgradePlans();

        assertEquals(1, eligible.size());
        assertEquals("Basic Monthly", eligible.get(0).planName());
    }

    @Test
    void getEligibleUpgradePlansShouldAllowAllWhenNoActiveSubscription() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of());
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc())
                .thenReturn(List.of(upgradePlan));

        assertEquals(1, planUpgradeService.getEligibleUpgradePlans().size());
    }

    @Test
    void getEligibleUpgradePlansShouldHandleSubscriptionWithoutPlan() {
        Subscription noPlan = new Subscription();
        noPlan.setId(11L);
        noPlan.setUser(user);
        noPlan.setPlan(null);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(noPlan));
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc())
                .thenReturn(List.of(upgradePlan));

        assertEquals(1, planUpgradeService.getEligibleUpgradePlans().size());
    }

    @Test
    void submitUpgradeRequestShouldCreateRequest() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> {
            PlanUpgradeRequest saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        PlanUpgradeDto dto = planUpgradeService.submitUpgradeRequest(
                new PlanUpgradeSubmitRequest(2L, "  Need more tokens  "));

        assertEquals(99L, dto.id());
        assertEquals("Welcome Pass", dto.currentPlan());
        assertEquals("Basic Monthly", dto.upgradePlan());
        assertEquals("Need more tokens", dto.reason());
        assertEquals("Started", dto.status());
        assertEquals("Requested", dto.action());
    }

    @Test
    void submitUpgradeRequestShouldUseUnknownWhenSubscriptionPlanNull() {
        subscription.setPlan(null);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> {
            PlanUpgradeRequest saved = inv.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        PlanUpgradeDto dto = planUpgradeService.submitUpgradeRequest(
                new PlanUpgradeSubmitRequest(2L, "reason"));

        assertEquals("Unknown", dto.currentPlan());
    }

    @Test
    void submitUpgradeRequestShouldThrowWhenActiveUpgradeExists() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> planUpgradeService.submitUpgradeRequest(new PlanUpgradeSubmitRequest(2L, "reason")));
        assertTrue(ex.getMessage().contains("already have an active plan upgrade"));
    }

    @Test
    void submitUpgradeRequestShouldThrowWhenActiveRefundExists() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> planUpgradeService.submitUpgradeRequest(new PlanUpgradeSubmitRequest(2L, "reason")));
        assertTrue(ex.getMessage().contains("refund request is active"));
    }

    @Test
    void submitUpgradeRequestShouldThrowWhenNoActiveSubscription() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> planUpgradeService.submitUpgradeRequest(new PlanUpgradeSubmitRequest(2L, "reason")));
    }

    @Test
    void submitUpgradeRequestShouldThrowWhenPlanNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));
        when(planRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> planUpgradeService.submitUpgradeRequest(new PlanUpgradeSubmitRequest(2L, "reason")));
    }

    @Test
    void submitUpgradeRequestShouldThrowWhenPlanInactive() {
        upgradePlan.setActive(false);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> planUpgradeService.submitUpgradeRequest(new PlanUpgradeSubmitRequest(2L, "reason")));
        assertTrue(ex.getMessage().contains("not available"));
    }

    @Test
    void submitUpgradeRequestShouldThrowWhenPlanTokensNotHigher() {
        upgradePlan.setTokensIncluded("50");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> planUpgradeService.submitUpgradeRequest(new PlanUpgradeSubmitRequest(2L, "reason")));
        assertTrue(ex.getMessage().contains("more tokens"));
    }

    @Test
    void submitUpgradeRequestShouldThrowWhenSamePlanSelected() {
        upgradePlan.setPlanName("Welcome Pass");
        upgradePlan.setTokensIncluded("300");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> planUpgradeService.submitUpgradeRequest(new PlanUpgradeSubmitRequest(2L, "reason")));
        assertTrue(ex.getMessage().contains("different plan"));
    }

    @Test
    void listUpgradesForAdminShouldMapRows() {
        when(upgradeRequestRepository.findAllWithDetails()).thenReturn(List.of(upgradeRequest));

        List<AdminPlanUpgradeListDto> result = planUpgradeService.listUpgradesForAdmin();

        assertEquals(1, result.size());
        assertEquals(22L, result.get(0).userId());
        assertEquals("UPG-1", result.get(0).formattedUpgradeId());
    }

    @Test
    void getUpgradeDetailForAdminShouldReturnDetail() {
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));

        AdminPlanUpgradeDetailDto detail = planUpgradeService.getUpgradeDetailForAdmin(1L);

        assertEquals("Sahil T", detail.userName());
        assertEquals("Basic Monthly", detail.upgradePlan());
    }

    @Test
    void getUpgradeDetailForAdminShouldThrowWhenMissing() {
        when(upgradeRequestRepository.findByIdWithDetails(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> planUpgradeService.getUpgradeDetailForAdmin(9L));
    }

    @Test
    void adminApproveShouldCalculateAndUpdateRequest() {
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(planRepository.findFirstByPlanNameIgnoreCase("Basic Monthly")).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminPlanUpgradeDetailDto detail = planUpgradeService.adminApprove(1L);

        assertEquals("InProgress", detail.status());
        assertEquals("Approved", detail.action());
        assertTrue(detail.extraToken() > 0);
        assertTrue(detail.extraAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void adminApproveShouldThrowWhenPlanMissing() {
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(planRepository.findFirstByPlanNameIgnoreCase("Basic Monthly")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planUpgradeService.adminApprove(1L));
    }

    @Test
    void adminApproveShouldRejectCompletedRequest() {
        upgradeRequest.setStatus(PlanUpgradeStatus.Completed);
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));

        assertThrows(BadRequestException.class, () -> planUpgradeService.adminApprove(1L));
    }

    @Test
    void adminApproveShouldRejectClosedRequest() {
        upgradeRequest.setStatus(PlanUpgradeStatus.Rejected);
        upgradeRequest.setAction(PlanUpgradeAction.Closed);
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));

        assertThrows(BadRequestException.class, () -> planUpgradeService.adminApprove(1L));
    }

    @Test
    void adminApproveShouldRejectAlreadyApprovedRequest() {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));

        assertThrows(BadRequestException.class, () -> planUpgradeService.adminApprove(1L));
    }

    @Test
    void adminRejectShouldCloseRequest() {
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminPlanUpgradeDetailDto detail = planUpgradeService.adminReject(1L);

        assertEquals("Rejected", detail.status());
        assertEquals("Closed", detail.action());
    }

    @Test
    void adminRejectShouldThrowWhenActionAlreadyClosed() {
        upgradeRequest.setAction(PlanUpgradeAction.Closed);
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));

        assertThrows(BadRequestException.class, () -> planUpgradeService.adminReject(1L));
    }

    @Test
    void calculateUpgradeCostShouldChargeFullWhenBelowFiftyTokens() {
        PlanUpgradeService.UpgradeCost cost =
                PlanUpgradeService.calculateUpgradeCost(40, 200, new BigDecimal("50.00"));

        assertEquals(200, cost.extraToken());
        assertEquals(new BigDecimal("50.00"), cost.extraAmount());
    }

    @Test
    void calculateUpgradeCostShouldProrateWhenAboveFiftyTokens() {
        // remaining = 100 - 50 = 50; required = 200 - 50 = 150; amount = 150/200 * 50 = 37.50
        PlanUpgradeService.UpgradeCost cost =
                PlanUpgradeService.calculateUpgradeCost(100, 200, new BigDecimal("50.00"));

        assertEquals(150, cost.extraToken());
        assertEquals(new BigDecimal("37.50"), cost.extraAmount());
    }

    @Test
    void calculateUpgradeCostShouldClampNegativeRequiredTokens() {
        PlanUpgradeService.UpgradeCost cost =
                PlanUpgradeService.calculateUpgradeCost(300, 200, new BigDecimal("50.00"));

        assertEquals(0, cost.extraToken());
        assertEquals(BigDecimal.ZERO, cost.extraAmount());
    }

    @Test
    void calculateUpgradeCostShouldHandleNullPrice() {
        PlanUpgradeService.UpgradeCost cost =
                PlanUpgradeService.calculateUpgradeCost(100, 200, null);

        assertEquals(150, cost.extraToken());
        assertEquals(BigDecimal.ZERO, cost.extraAmount());
    }

    @Test
    void calculateUpgradeCostShouldThrowWhenPlanTokensInvalid() {
        assertThrows(BadRequestException.class,
                () -> PlanUpgradeService.calculateUpgradeCost(100, 0, new BigDecimal("50.00")));
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenNotPaid() {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("unpaid");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
        assertTrue(ex.getMessage().contains("not paid"));
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenUserMismatch() {
        Session session = paidSession("22", "2", "1");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 99L));
        assertTrue(ex.getMessage().contains("does not belong"));
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenMetadataMissing() {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getMetadata()).thenReturn(new HashMap<>());

        assertThrows(BadRequestException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, null));
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenMetadataInvalid() {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getMetadata()).thenReturn(Map.of(
                "userId", "abc", "planId", "2", "upgradeRequestId", "1"));

        assertThrows(BadRequestException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, null));
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenUserNotFound() {
        Session session = paidSession("22", "2", "1");
        when(userRepository.findById(22L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenPlanNotFound() {
        Session session = paidSession("22", "2", "1");
        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenUpgradeMissing() {
        Session session = paidSession("22", "2", "1");
        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenUpgradeBelongsToOtherUser() {
        User other = new User();
        other.setId(99L);
        upgradeRequest.setUser(other);

        Session session = paidSession("22", "2", "1");
        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));

        assertThrows(BadRequestException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
    }

    @Test
    void fulfillUpgradeCheckoutShouldReturnExistingWhenAlreadyCompleted() {
        Payment existingPayment = new Payment();
        existingPayment.setId(77L);
        existingPayment.setAmount(new BigDecimal("37.50"));
        existingPayment.setCurrency("usd");
        existingPayment.setStatus("succeeded");
        existingPayment.setCreatedAt(LocalDateTime.now());
        existingPayment.setMethod("VISA - 4242");

        upgradeRequest.setStatus(PlanUpgradeStatus.Completed);
        upgradeRequest.setAction(PlanUpgradeAction.Closed);
        upgradeRequest.setPayment(existingPayment);

        Session session = paidSession("22", "2", "1");
        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);

        assertEquals("succeeded", result.paymentStatus());
        assertNotNull(result.receipt());
        assertNotNull(result.subscription());
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenNotApproved() {
        Session session = paidSession("22", "2", "1");
        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));

        assertThrows(BadRequestException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
    }

    @Test
    void fulfillUpgradeCheckoutShouldCreateSubscriptionPaymentAndCreditTokens() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(50);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_upgrade_1");
        when(session.getPaymentIntent()).thenReturn("pi_123");
        when(session.getAmountTotal()).thenReturn(3750L);
        when(session.getCurrency()).thenReturn("usd");
        when(session.getCustomer()).thenReturn("cus_abc");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.findByStripeSubscriptionId("cs_upgrade_1")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.empty());

        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getLatestCharge()).thenReturn("ch_123");
        when(intent.getPaymentMethod()).thenReturn("pm_123");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(intent);

        Charge charge = mock(Charge.class);
        when(charge.getReceiptUrl()).thenReturn("https://receipt");
        chargeStatic.when(() -> Charge.retrieve("ch_123")).thenReturn(charge);

        PaymentMethod pm = mock(PaymentMethod.class);
        PaymentMethod.Card card = mock(PaymentMethod.Card.class);
        when(pm.getType()).thenReturn("card");
        when(pm.getCard()).thenReturn(card);
        when(card.getBrand()).thenReturn("visa");
        when(card.getLast4()).thenReturn("4242");
        paymentMethodStatic.when(() -> PaymentMethod.retrieve("pm_123")).thenReturn(pm);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(70L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);

        assertEquals("succeeded", result.paymentStatus());
        assertEquals(150, user.getUserTokens());
        assertEquals(PlanUpgradeStatus.Completed, upgradeRequest.getStatus());
        assertEquals(PlanUpgradeAction.Closed, upgradeRequest.getAction());
        verify(subscriptionRepository, atLeastOnce()).save(argThat(s -> "cancelled".equals(s.getStatus())));
        assertEquals("Basic Monthly", result.subscription().planName());
        assertEquals("VISA - 4242", result.receipt().paymentMethod());
    }

    @Test
    void fulfillUpgradeCheckoutShouldReuseExistingSubscriptionAndPayment() {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(0);

        Subscription existingSub = new Subscription();
        existingSub.setId(60L);
        existingSub.setUser(user);
        existingSub.setPlan(upgradePlan);
        existingSub.setStatus("active");
        existingSub.setStripeSubscriptionId("cs_upgrade_1");
        existingSub.setCurrentPeriodStart(LocalDateTime.now());
        existingSub.setCurrentPeriodEnd(LocalDateTime.now().plusDays(30));

        Payment existingPayment = new Payment();
        existingPayment.setId(80L);
        existingPayment.setUser(user);
        existingPayment.setStatus("succeeded");
        existingPayment.setAmount(new BigDecimal("10.00"));
        existingPayment.setCurrency("usd");
        existingPayment.setTokensCredited(false);
        existingPayment.setCreatedAt(LocalDateTime.now());

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_upgrade_1");
        when(session.getPaymentIntent()).thenReturn("pi_exist");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(existingSub));
        when(subscriptionRepository.findByStripeSubscriptionId("cs_upgrade_1"))
                .thenReturn(Optional.of(existingSub));
        when(paymentRepository.findByStripePaymentIntentId("pi_exist"))
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, null);

        assertTrue(existingPayment.isTokensCredited());
        assertEquals(60L, result.subscription().id());
        assertEquals(PlanUpgradeStatus.Completed, upgradeRequest.getStatus());
    }

    @Test
    void fulfillUpgradeCheckoutShouldLinkSubscriptionWhenExistingPaymentHasNone() {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(null);

        Subscription existingSub = new Subscription();
        existingSub.setId(61L);
        existingSub.setUser(user);
        existingSub.setPlan(upgradePlan);
        existingSub.setStatus("active");
        existingSub.setStripeSubscriptionId("cs_upgrade_2");
        existingSub.setCurrentPeriodStart(LocalDateTime.now());
        existingSub.setCurrentPeriodEnd(LocalDateTime.now().plusDays(30));

        Payment existingPayment = new Payment();
        existingPayment.setId(81L);
        existingPayment.setUser(user);
        existingPayment.setStatus("succeeded");
        existingPayment.setAmount(new BigDecimal("10.00"));
        existingPayment.setCurrency("usd");
        existingPayment.setTokensCredited(true);
        existingPayment.setCreatedAt(LocalDateTime.now());

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_upgrade_2");
        when(session.getPaymentIntent()).thenReturn("pi_link");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_upgrade_2"))
                .thenReturn(Optional.of(existingSub));
        when(paymentRepository.findByStripePaymentIntentId("pi_link"))
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        planUpgradeService.fulfillUpgradeCheckout(session, 22L);

        assertEquals(existingSub, existingPayment.getSubscription());
        verify(paymentRepository).save(existingPayment);
    }

    @Test
    void fulfillUpgradeCheckoutShouldHandleSubscriptionRaceAndPaymentRace() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(10);

        Subscription raced = new Subscription();
        raced.setId(90L);
        raced.setUser(user);
        raced.setPlan(upgradePlan);
        raced.setStatus("active");
        raced.setStripeSubscriptionId("cs_race");
        raced.setCurrentPeriodStart(LocalDateTime.now());
        raced.setCurrentPeriodEnd(LocalDateTime.now().plusDays(30));

        Payment racedPayment = new Payment();
        racedPayment.setId(91L);
        racedPayment.setUser(user);
        racedPayment.setSubscription(raced);
        racedPayment.setStatus("succeeded");
        racedPayment.setAmount(new BigDecimal("5.00"));
        racedPayment.setCurrency("usd");
        racedPayment.setTokensCredited(false);
        racedPayment.setCreatedAt(LocalDateTime.now());

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_race");
        when(session.getPaymentIntent()).thenReturn("pi_race");
        when(session.getAmountTotal()).thenReturn(500L);
        when(session.getCurrency()).thenReturn(null);
        when(session.getCustomer()).thenReturn("  ");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_race"))
                .thenReturn(Optional.empty(), Optional.of(raced));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        when(paymentRepository.findByStripePaymentIntentId("pi_race"))
                .thenReturn(Optional.empty(), Optional.of(racedPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("dup payment"))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getLatestCharge()).thenReturn("bad_charge");
        when(intent.getPaymentMethod()).thenReturn(null);
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_race")).thenReturn(intent);

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);

        assertEquals(90L, result.subscription().id());
        assertEquals(110, user.getUserTokens());
    }

    @Test
    void fulfillUpgradeCheckoutShouldRethrowPaymentRaceWhenIntentMissing() {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_no_intent");
        when(session.getPaymentIntent()).thenReturn(null);
        when(session.getAmountTotal()).thenReturn(null);
        when(session.getCurrency()).thenReturn("usd");
        when(session.getCustomer()).thenReturn(null);

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_no_intent")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        assertThrows(DataIntegrityViolationException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
    }

    @Test
    void fulfillUpgradeCheckoutShouldHandleStripeEnrichmentFailures() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_stripe_fail");
        when(session.getPaymentIntent()).thenReturn("pi_fail");
        when(session.getAmountTotal()).thenReturn(100L);
        when(session.getCurrency()).thenReturn("usd");
        when(session.getCustomer()).thenReturn("cus_x");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_stripe_fail")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(101L);
            return s;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_fail")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(102L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        StripeException stripeException = mock(StripeException.class);
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_fail")).thenThrow(stripeException);

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);

        assertEquals("succeeded", result.paymentStatus());
        assertNull(result.receipt().paymentMethod());
    }

    @Test
    void fulfillUpgradeCheckoutShouldHandlePaymentMethodWithoutBrandAndChargeUrlFailure() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_pm");
        when(session.getPaymentIntent()).thenReturn("pi_pm");
        when(session.getAmountTotal()).thenReturn(200L);
        when(session.getCurrency()).thenReturn("usd");
        when(session.getCustomer()).thenReturn("cus_pm");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_pm")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(110L);
            return s;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_pm")).thenReturn(Optional.empty());
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(111L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getLatestCharge()).thenReturn("ch_pm");
        when(intent.getPaymentMethod()).thenReturn("pm_pm");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_pm")).thenReturn(intent);

        StripeException chargeEx = mock(StripeException.class);
        chargeStatic.when(() -> Charge.retrieve("ch_pm")).thenThrow(chargeEx);

        PaymentMethod pm = mock(PaymentMethod.class);
        PaymentMethod.Card card = mock(PaymentMethod.Card.class);
        when(pm.getType()).thenReturn("card");
        when(pm.getCard()).thenReturn(card);
        when(card.getBrand()).thenReturn(null);
        when(card.getLast4()).thenReturn("1111");
        paymentMethodStatic.when(() -> PaymentMethod.retrieve("pm_pm")).thenReturn(pm);

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);

        assertEquals("Card - 1111", result.receipt().paymentMethod());
        assertNull(paymentCaptor.getValue().getReceiptUrl());
    }

    @Test
    void fulfillUpgradeCheckoutShouldHandlePaymentMethodRetrieveFailure() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_pm_fail");
        when(session.getPaymentIntent()).thenReturn("pi_pm_fail");
        when(session.getAmountTotal()).thenReturn(200L);
        when(session.getCurrency()).thenReturn("usd");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_pm_fail")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(120L);
            return s;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_pm_fail")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(121L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getLatestCharge()).thenReturn(null);
        when(intent.getPaymentMethod()).thenReturn("pm_fail");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_pm_fail")).thenReturn(intent);

        StripeException pmEx = mock(StripeException.class);
        paymentMethodStatic.when(() -> PaymentMethod.retrieve("pm_fail")).thenThrow(pmEx);

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);
        assertNull(result.receipt().paymentMethod());
    }

    @Test
    void fulfillUpgradeCheckoutShouldIgnoreNonCardPaymentMethod() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_link");
        when(session.getPaymentIntent()).thenReturn("pi_link_pm");
        when(session.getAmountTotal()).thenReturn(200L);
        when(session.getCurrency()).thenReturn("usd");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_link")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(130L);
            return s;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_link_pm")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(131L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getLatestCharge()).thenReturn(null);
        when(intent.getPaymentMethod()).thenReturn("pm_link");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_link_pm")).thenReturn(intent);

        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.getType()).thenReturn("link");
        paymentMethodStatic.when(() -> PaymentMethod.retrieve("pm_link")).thenReturn(pm);

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);
        assertNull(result.receipt().paymentMethod());
    }

    @Test
    void fulfillUpgradeCheckoutShouldRethrowWhenSubscriptionRaceLookupFails() {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_sub_race_fail");
        when(session.getCustomer()).thenReturn("cus_y");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_sub_race_fail"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenThrow(new DataIntegrityViolationException("dup sub"));

        assertThrows(DataIntegrityViolationException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
    }

    @Test
    void fulfillUpgradeCheckoutShouldRethrowWhenPaymentRaceLookupFails() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_pay_race_fail");
        when(session.getPaymentIntent()).thenReturn("pi_pay_race_fail");
        when(session.getAmountTotal()).thenReturn(100L);
        when(session.getCurrency()).thenReturn("usd");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_pay_race_fail")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(140L);
            return s;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_pay_race_fail"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("dup pay"));

        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getLatestCharge()).thenReturn(null);
        when(intent.getPaymentMethod()).thenReturn(null);
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_pay_race_fail")).thenReturn(intent);

        assertThrows(DataIntegrityViolationException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenMetadataBlank() {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", "  ");
        metadata.put("planId", "2");
        metadata.put("upgradeRequestId", "1");
        when(session.getMetadata()).thenReturn(metadata);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, null));
        assertTrue(ex.getMessage().contains("Missing Stripe metadata field"));
    }

    @Test
    void fulfillUpgradeCheckoutShouldReturnNullSubscriptionWhenCompletedWithoutActiveSub() {
        Payment existingPayment = new Payment();
        existingPayment.setId(77L);
        existingPayment.setAmount(new BigDecimal("37.50"));
        existingPayment.setCurrency("usd");
        existingPayment.setStatus("succeeded");
        existingPayment.setCreatedAt(LocalDateTime.now());

        upgradeRequest.setStatus(PlanUpgradeStatus.Completed);
        upgradeRequest.setAction(PlanUpgradeAction.Closed);
        upgradeRequest.setPayment(existingPayment);

        Session session = paidSession("22", "2", "1");
        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of());

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);

        assertNull(result.subscription());
        assertEquals("succeeded", result.paymentStatus());
    }

    @Test
    void submitUpgradeRequestShouldAllowWhenUpgradePlanNameIsNull() {
        upgradePlan.setPlanName(null);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(upgradeRequestRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(refundRepository.existsByUser_IdAndStatusNotIn(eq(22L), anyList())).thenReturn(false);
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> {
            PlanUpgradeRequest saved = inv.getArgument(0);
            saved.setId(44L);
            return saved;
        });

        PlanUpgradeDto dto = planUpgradeService.submitUpgradeRequest(
                new PlanUpgradeSubmitRequest(2L, "reason"));

        assertEquals(44L, dto.id());
        assertNull(dto.upgradePlan());
    }

    @Test
    void fulfillUpgradeCheckoutShouldHandleCardPaymentMethodWithNullCardObject() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_null_card");
        when(session.getPaymentIntent()).thenReturn("pi_null_card");
        when(session.getAmountTotal()).thenReturn(200L);
        when(session.getCurrency()).thenReturn("usd");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_null_card")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(150L);
            return s;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_null_card")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(151L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getLatestCharge()).thenReturn("");
        when(intent.getPaymentMethod()).thenReturn("pm_null_card");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_null_card")).thenReturn(intent);

        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.getType()).thenReturn("card");
        when(pm.getCard()).thenReturn(null);
        paymentMethodStatic.when(() -> PaymentMethod.retrieve("pm_null_card")).thenReturn(pm);

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);
        assertNull(result.receipt().paymentMethod());
    }

    @Test
    void fulfillUpgradeCheckoutShouldHandleNullPaymentMethodObject() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_null_pm");
        when(session.getPaymentIntent()).thenReturn("pi_null_pm");
        when(session.getAmountTotal()).thenReturn(200L);
        when(session.getCurrency()).thenReturn("usd");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_null_pm")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(160L);
            return s;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_null_pm")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(161L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getLatestCharge()).thenReturn(null);
        when(intent.getPaymentMethod()).thenReturn("pm_null");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_null_pm")).thenReturn(intent);
        paymentMethodStatic.when(() -> PaymentMethod.retrieve("pm_null")).thenReturn(null);

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);
        assertNull(result.receipt().paymentMethod());
    }

    @Test
    void calculateUpgradeCostShouldReturnZeroAmountWhenPriceIsZero() {
        PlanUpgradeService.UpgradeCost cost =
                PlanUpgradeService.calculateUpgradeCost(100, 200, BigDecimal.ZERO);

        assertEquals(150, cost.extraToken());
        assertEquals(BigDecimal.ZERO, cost.extraAmount());
    }

    @Test
    void fulfillUpgradeCheckoutShouldSkipCancellingSubscriptionMatchingCheckoutSession() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Subscription matching = new Subscription();
        matching.setId(200L);
        matching.setUser(user);
        matching.setPlan(upgradePlan);
        matching.setStatus("active");
        matching.setStripeSubscriptionId("cs_same");
        matching.setCurrentPeriodStart(LocalDateTime.now());
        matching.setCurrentPeriodEnd(LocalDateTime.now().plusDays(30));

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_same");
        when(session.getPaymentIntent()).thenReturn(null);
        when(session.getAmountTotal()).thenReturn(100L);
        when(session.getCurrency()).thenReturn("usd");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(matching));
        when(subscriptionRepository.findByStripeSubscriptionId("cs_same"))
                .thenReturn(Optional.of(matching));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(201L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);

        assertEquals(200L, result.subscription().id());
        verify(subscriptionRepository, never()).save(argThat(s -> "cancelled".equals(s.getStatus())));
    }

    @Test
    void fulfillUpgradeCheckoutShouldHandleBlankChargeIdForReceipt() throws Exception {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_blank_ch");
        when(session.getPaymentIntent()).thenReturn("pi_blank_ch");
        when(session.getAmountTotal()).thenReturn(100L);
        when(session.getCurrency()).thenReturn("usd");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_blank_ch")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(210L);
            return s;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_blank_ch")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(211L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getLatestCharge()).thenReturn("   ");
        when(intent.getPaymentMethod()).thenReturn(null);
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_blank_ch")).thenReturn(intent);

        CheckoutConfirmDto result = planUpgradeService.fulfillUpgradeCheckout(session, 22L);
        assertNull(result.receipt().receiptUrl());
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenCompletedWithoutPaymentLinked() {
        upgradeRequest.setStatus(PlanUpgradeStatus.Completed);
        upgradeRequest.setAction(PlanUpgradeAction.Closed);
        upgradeRequest.setPayment(null);

        Session session = paidSession("22", "2", "1");
        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
        assertTrue(ex.getMessage().contains("not approved for payment"));
    }

    @Test
    void fulfillUpgradeCheckoutShouldThrowWhenInProgressButNotApprovedAction() {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Requested);

        Session session = paidSession("22", "2", "1");
        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));

        assertThrows(BadRequestException.class,
                () -> planUpgradeService.fulfillUpgradeCheckout(session, 22L));
    }

    @Test
    void fulfillUpgradeCheckoutShouldNotRecreditWhenPaymentAlreadyCredited() {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(40);

        Subscription existingSub = new Subscription();
        existingSub.setId(300L);
        existingSub.setUser(user);
        existingSub.setPlan(upgradePlan);
        existingSub.setStatus("active");
        existingSub.setStripeSubscriptionId("cs_credited");
        existingSub.setCurrentPeriodStart(LocalDateTime.now());
        existingSub.setCurrentPeriodEnd(LocalDateTime.now().plusDays(30));

        Payment existingPayment = new Payment();
        existingPayment.setId(301L);
        existingPayment.setUser(user);
        existingPayment.setSubscription(existingSub);
        existingPayment.setStatus("succeeded");
        existingPayment.setAmount(new BigDecimal("10.00"));
        existingPayment.setCurrency("usd");
        existingPayment.setTokensCredited(true);
        existingPayment.setCreatedAt(LocalDateTime.now());

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn("cs_credited");
        when(session.getPaymentIntent()).thenReturn("pi_credited");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any())).thenReturn(List.of());
        when(subscriptionRepository.findByStripeSubscriptionId("cs_credited"))
                .thenReturn(Optional.of(existingSub));
        when(paymentRepository.findByStripePaymentIntentId("pi_credited"))
                .thenReturn(Optional.of(existingPayment));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        int tokensBefore = user.getUserTokens();
        planUpgradeService.fulfillUpgradeCheckout(session, 22L);

        assertEquals(tokensBefore, user.getUserTokens());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void fulfillUpgradeCheckoutShouldCancelWhenCheckoutSessionIdIsNull() {
        upgradeRequest.setStatus(PlanUpgradeStatus.InProgress);
        upgradeRequest.setAction(PlanUpgradeAction.Approved);
        upgradeRequest.setExtraToken(5);

        Session session = paidSession("22", "2", "1");
        when(session.getId()).thenReturn(null);
        when(session.getPaymentIntent()).thenReturn(null);
        when(session.getAmountTotal()).thenReturn(100L);
        when(session.getCurrency()).thenReturn("usd");
        when(session.getCustomer()).thenReturn("cus_null_id");

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(upgradePlan));
        when(upgradeRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(upgradeRequest));
        when(subscriptionRepository.findActiveSubscriptionsForUser(eq(22L), any()))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.findByStripeSubscriptionId(null)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(310L);
            }
            return s;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(311L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(upgradeRequestRepository.save(any(PlanUpgradeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        planUpgradeService.fulfillUpgradeCheckout(session, 22L);

        assertEquals("cancelled", subscription.getStatus());
    }

    @Test
    void fetchPaymentMethodLabelShouldReturnNullForNullIntent() {
        Object result = ReflectionTestUtils.invokeMethod(
                planUpgradeService, "fetchPaymentMethodLabel", new Object[]{null});
        assertNull(result);
    }

    private Session paidSession(String userId, String planId, String upgradeRequestId) {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getMetadata()).thenReturn(Map.of(
                "userId", userId,
                "planId", planId,
                "upgradeRequestId", upgradeRequestId
        ));
        return session;
    }
}
