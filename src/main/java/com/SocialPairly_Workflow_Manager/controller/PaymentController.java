package com.SocialPairly_Workflow_Manager.controller;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.dto.CheckoutConfirmDto;
import com.SocialPairly_Workflow_Manager.dto.PaymentRequestDTO;
import com.SocialPairly_Workflow_Manager.dto.PaymentResponseDTO;
import com.SocialPairly_Workflow_Manager.dto.PaymentStatusDto;
import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.service.CheckoutFulfillmentService;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService stripeService;
    private final CurrentUserService currentUserService;
    private final PaymentRepository paymentRepository;
    private final CheckoutFulfillmentService checkoutFulfillmentService;

    public PaymentController(PaymentService stripeService,
                             CurrentUserService currentUserService,
                             PaymentRepository paymentRepository,
                             CheckoutFulfillmentService checkoutFulfillmentService) {
        this.stripeService = stripeService;
        this.currentUserService = currentUserService;
        this.paymentRepository = paymentRepository;
        this.checkoutFulfillmentService = checkoutFulfillmentService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<PaymentResponseDTO> checkoutProducts(@RequestBody PaymentRequestDTO productRequest) {
        log.info("Payment checkout requested for product={} planId={}", productRequest.getName(), productRequest.getPlanId());
        PaymentResponseDTO stripeResponse = stripeService.checkoutProducts(productRequest);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(stripeResponse);
    }

    @PostMapping("/confirm-session")
    public ResponseEntity<CheckoutConfirmDto> confirmCheckoutSession(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            throw new BadRequestException("sessionId is required");
        }
        User user = currentUserService.getCurrentUser();
        log.info("Confirming checkout session {} for userId={}", sessionId, user.getId());
        CheckoutConfirmDto result = checkoutFulfillmentService.fulfillCheckoutSession(sessionId, user.getId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/latest-status")
    public ResponseEntity<?> getLatestPaymentStatus() {
        User user = currentUserService.getCurrentUser();
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return paymentRepository.findFirstByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), since)
                .<ResponseEntity<?>>map(payment -> ResponseEntity.ok(toStatusDto(payment)))
                .orElseGet(() -> ResponseEntity.ok(Map.of("status", "none")));
    }

    private PaymentStatusDto toStatusDto(Payment payment) {
        return new PaymentStatusDto(
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt()
        );
    }
}
