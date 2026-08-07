package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AdminSubscriptionDto;
import com.SocialPairly_Workflow_Manager.dto.SubscriptionDto;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
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
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User user;
    private Plan plan;
    private Subscription subscription;
    private Payment payment;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        plan = new Plan();
        plan.setId(5L);
        plan.setPlanName("Pro");
        plan.setPlanType("MONTHLY");
        plan.setDurationDays(30);
        plan.setAmount(new BigDecimal("29.99"));

        subscription = new Subscription();
        subscription.setId(10L);
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus("active");
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(30));

        payment = new Payment();
        payment.setId(20L);
        payment.setUser(user);
        payment.setSubscription(subscription);
    }

    @Test
    void getCurrentSubscriptionShouldReturnEmptyWhenNoneActive() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(subscriptionRepository.findActiveSubscriptionForUser(eq(1L), any()))
                .thenReturn(Optional.empty());

        assertTrue(subscriptionService.getCurrentSubscription().isEmpty());
    }

    @Test
    void getCurrentSubscriptionShouldReturnDtoWhenActive() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(subscriptionRepository.findActiveSubscriptionForUser(eq(1L), any()))
                .thenReturn(Optional.of(subscription));

        Optional<SubscriptionDto> result = subscriptionService.getCurrentSubscription();

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().id());
        assertEquals("Pro", result.get().planName());
    }

    @Test
    void getAllSubscriptionsForAdminShouldReturnMappedList() {
        when(subscriptionRepository.findAllWithUserAndPlan()).thenReturn(List.of(subscription));

        List<AdminSubscriptionDto> result = subscriptionService.getAllSubscriptionsForAdmin();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
        assertEquals("user@example.com", result.get(0).userName());
    }

    @Test
    void removeSubscriptionShouldUnlinkPaymentsAndDeleteSubscription() {
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findBySubscription_Id(10L)).thenReturn(List.of(payment));

        subscriptionService.removeSubscription(10L);

        assertNull(payment.getSubscription());
        verify(paymentRepository).saveAll(List.of(payment));
        verify(subscriptionRepository).delete(subscription);
    }

    @Test
    void removeSubscriptionShouldThrowWhenNotFound() {
        when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.removeSubscription(99L));
        assertEquals("Subscription not found: 99", ex.getMessage());
        verify(subscriptionRepository, never()).delete(any());
    }
}
