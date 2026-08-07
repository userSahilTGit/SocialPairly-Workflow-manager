package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.CheckoutConfirmDto;
import com.SocialPairly_Workflow_Manager.dto.PaymentRequestDTO;
import com.SocialPairly_Workflow_Manager.dto.PaymentResponseDTO;
import com.SocialPairly_Workflow_Manager.dto.PaymentStatusDto;
import com.SocialPairly_Workflow_Manager.dto.ReceiptDto;
import com.SocialPairly_Workflow_Manager.dto.SubscriptionDto;
import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.service.CheckoutFulfillmentService;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CheckoutFulfillmentService checkoutFulfillmentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void checkoutProductsShouldReturnOkResponseWhenServiceReturnsResponse() {
        PaymentRequestDTO request = new PaymentRequestDTO(2000L, 1L, "Book", "USD", 1L);
        PaymentResponseDTO serviceResponse = PaymentResponseDTO.builder()
                .status("SUCCESS")
                .message("Payment session created ")
                .sessionId("sess_456")
                .sessionUrl("https://checkout.stripe.com/session/sess_456")
                .build();

        when(paymentService.checkoutProducts(any(PaymentRequestDTO.class))).thenReturn(serviceResponse);

        ResponseEntity<PaymentResponseDTO> response = paymentController.checkoutProducts(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        assertEquals("sess_456", response.getBody().getSessionId());
    }

    @Test
    void confirmCheckoutSessionShouldFulfillSessionForCurrentUser() {
        User user = new User();
        user.setId(42L);

        CheckoutConfirmDto confirmDto = new CheckoutConfirmDto(
                new SubscriptionDto(1L, 5L, "Pro", "MONTHLY", 30, new BigDecimal("29.99"),
                        "active", LocalDateTime.now(), LocalDateTime.now().plusDays(30), false),
                new ReceiptDto(null, "Pro", new BigDecimal("29.99"), "usd", LocalDateTime.now(),
                        "user@test.com", null, "SocialPairly", "support@test.com", "VISA - 4242"),
                "succeeded"
        );

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(checkoutFulfillmentService.fulfillCheckoutSession("sess_confirm", 42L)).thenReturn(confirmDto);

        ResponseEntity<CheckoutConfirmDto> response = paymentController.confirmCheckoutSession(
                Map.of("sessionId", "sess_confirm"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(confirmDto, response.getBody());
        assertEquals("succeeded", response.getBody().paymentStatus());
        verify(checkoutFulfillmentService).fulfillCheckoutSession("sess_confirm", 42L);
    }

    @Test
    void confirmCheckoutSessionShouldThrowWhenSessionIdMissing() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> paymentController.confirmCheckoutSession(Map.of()));
        assertEquals("sessionId is required", ex.getMessage());

        BadRequestException blankEx = assertThrows(BadRequestException.class,
                () -> paymentController.confirmCheckoutSession(Map.of("sessionId", "   ")));
        assertEquals("sessionId is required", blankEx.getMessage());

        verifyNoInteractions(checkoutFulfillmentService);
    }

    @Test
    void confirmCheckoutSessionShouldThrowWhenSessionIdNull() {
        assertThrows(BadRequestException.class,
                () -> paymentController.confirmCheckoutSession(Map.of("sessionId", "")));
    }

    @Test
    void getLatestPaymentStatusShouldReturnStatusDtoWhenRecentPaymentExists() {
        User user = new User();
        user.setId(7L);

        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 7, 10, 0);
        Payment payment = new Payment();
        payment.setStatus("succeeded");
        payment.setAmount(new BigDecimal("49.99"));
        payment.setCurrency("usd");
        payment.setCreatedAt(createdAt);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(paymentRepository.findFirstByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(7L), any(LocalDateTime.class))).thenReturn(Optional.of(payment));

        ResponseEntity<?> response = paymentController.getLatestPaymentStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PaymentStatusDto statusDto = (PaymentStatusDto) response.getBody();
        assertEquals("succeeded", statusDto.status());
        assertEquals(new BigDecimal("49.99"), statusDto.amount());
        assertEquals("usd", statusDto.currency());
        assertEquals(createdAt, statusDto.createdAt());
    }

    @Test
    void getLatestPaymentStatusShouldReturnNoneWhenNoRecentPayment() {
        User user = new User();
        user.setId(8L);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(paymentRepository.findFirstByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(8L), any(LocalDateTime.class))).thenReturn(Optional.empty());

        ResponseEntity<?> response = paymentController.getLatestPaymentStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("none", ((Map<?, ?>) response.getBody()).get("status"));
    }
}
