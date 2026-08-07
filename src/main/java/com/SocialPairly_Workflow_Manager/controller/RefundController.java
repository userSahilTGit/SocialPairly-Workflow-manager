package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.service.RefundService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private static final Logger log = LoggerFactory.getLogger(RefundController.class);
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentRefund() {
        return refundService.getCurrentUserRefund()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(Map.of("active", false)));
    }

    @PostMapping
    public ResponseEntity<RefundDto> submitRefund(@Valid @RequestBody RefundRequestDto request) {
        log.info("Refund request submitted");
        return ResponseEntity.ok(refundService.submitRefundRequest(request));
    }

    @PostMapping("/{id}/slot")
    public ResponseEntity<RefundDto> submitSlot(@PathVariable Long id,
                                                 @Valid @RequestBody SlotRequestDto request) {
        return ResponseEntity.ok(refundService.submitSlot(id, request));
    }

    @PostMapping("/{id}/bank-details")
    public ResponseEntity<RefundDto> submitBankDetails(@PathVariable Long id,
                                                        @Valid @RequestBody BankDetailsRequestDto request) {
        return ResponseEntity.ok(refundService.submitBankDetails(id, request));
    }
}
