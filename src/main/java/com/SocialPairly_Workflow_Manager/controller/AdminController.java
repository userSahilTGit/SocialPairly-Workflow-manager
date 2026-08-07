package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AdminPaymentDto;
import com.SocialPairly_Workflow_Manager.dto.AdminRefundDetailDto;
import com.SocialPairly_Workflow_Manager.dto.AdminRefundListDto;
import com.SocialPairly_Workflow_Manager.dto.AdminStatsDto;
import com.SocialPairly_Workflow_Manager.dto.AdminSubscriptionDto;
import com.SocialPairly_Workflow_Manager.dto.QuestionRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.service.AdminService;
import com.SocialPairly_Workflow_Manager.service.QuestionService;
import com.SocialPairly_Workflow_Manager.service.RefundService;
import com.SocialPairly_Workflow_Manager.service.SubscriptionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;
    private final QuestionService questionService;
    private final SubscriptionService subscriptionService;
    private final RefundService refundService;

    public AdminController(AdminService adminService,
                            QuestionService questionService,
                            SubscriptionService subscriptionService,
                            RefundService refundService) {
        this.adminService = adminService;
        this.questionService = questionService;
        this.subscriptionService = subscriptionService;
        this.refundService = refundService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        log.info("Admin stats requested");
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @GetMapping("/payments")
    public ResponseEntity<List<AdminPaymentDto>> getPayments() {
        log.info("Admin payments list requested");
        return ResponseEntity.ok(adminService.listPayments());
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<AdminSubscriptionDto>> getSubscriptions() {
        log.info("Admin subscriptions list requested");
        return ResponseEntity.ok(subscriptionService.getAllSubscriptionsForAdmin());
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Map<String, String>> removeSubscription(@PathVariable Long id) {
        log.info("Admin removing subscription id={}", id);
        subscriptionService.removeSubscription(id);
        return ResponseEntity.ok(Map.of("message", "Subscription removed successfully"));
    }

    // ----- Refund management -----

    @GetMapping("/refunds")
    public ResponseEntity<List<AdminRefundListDto>> getRefunds() {
        log.info("Admin refunds list requested");
        return ResponseEntity.ok(refundService.listRefundsForAdmin());
    }

    @GetMapping("/refunds/{id}")
    public ResponseEntity<AdminRefundDetailDto> getRefundDetail(@PathVariable Long id) {
        return ResponseEntity.ok(refundService.getRefundDetailForAdmin(id));
    }

    @PostMapping("/refunds/{id}/request-call")
    public ResponseEntity<AdminRefundDetailDto> requestCall(@PathVariable Long id) {
        log.info("Admin requesting call for refund id={}", id);
        return ResponseEntity.ok(refundService.adminRequestCall(id));
    }

    @PostMapping("/refunds/{id}/approve")
    public ResponseEntity<AdminRefundDetailDto> approveRefund(@PathVariable Long id) {
        log.info("Admin approving refund id={}", id);
        return ResponseEntity.ok(refundService.adminApprove(id));
    }

    @PostMapping("/refunds/{id}/close")
    public ResponseEntity<AdminRefundDetailDto> closeRefund(@PathVariable Long id) {
        log.info("Admin closing refund id={}", id);
        return ResponseEntity.ok(refundService.adminClose(id));
    }

    @PostMapping("/refunds/{id}/complete")
    public ResponseEntity<AdminRefundDetailDto> completeRefund(@PathVariable Long id) {
        log.info("Admin completing refund payout id={}", id);
        return ResponseEntity.ok(refundService.adminCompletePayout(id));
    }

    // ----- Question management -----

    @GetMapping("/questions")
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionService.findAll());
    }

    @PostMapping("/questions")
    public ResponseEntity<Question> createQuestion(@Valid @RequestBody QuestionRequest request) {
        log.info("Admin creating question category={} active={}", request.category(), request.active());
        return ResponseEntity.ok(questionService.create(request));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(questionService.update(id, request));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Map<String, String>> deleteQuestion(@PathVariable Long id) {
        log.info("Admin deleting question id={}", id);
        questionService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Question deleted"));
    }
}