package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.PlanDto;
import com.SocialPairly_Workflow_Manager.dto.PlanUpgradeDto;
import com.SocialPairly_Workflow_Manager.dto.PlanUpgradeSubmitRequest;
import com.SocialPairly_Workflow_Manager.dto.PaymentResponseDTO;
import com.SocialPairly_Workflow_Manager.service.PaymentService;
import com.SocialPairly_Workflow_Manager.service.PlanUpgradeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upgrades")
public class PlanUpgradeController {

    private static final Logger log = LoggerFactory.getLogger(PlanUpgradeController.class);

    private final PlanUpgradeService planUpgradeService;
    private final PaymentService paymentService;

    public PlanUpgradeController(PlanUpgradeService planUpgradeService, PaymentService paymentService) {
        this.planUpgradeService = planUpgradeService;
        this.paymentService = paymentService;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUpgrade() {
        return planUpgradeService.getCurrentUserUpgrade()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(Map.of("active", false)));
    }

    @GetMapping("/eligible-plans")
    public ResponseEntity<List<PlanDto>> getEligiblePlans() {
        return ResponseEntity.ok(planUpgradeService.getEligibleUpgradePlans());
    }

    @PostMapping
    public ResponseEntity<PlanUpgradeDto> submitUpgrade(@Valid @RequestBody PlanUpgradeSubmitRequest request) {
        log.info("Plan upgrade request submitted for planId={}", request.upgradePlanId());
        return ResponseEntity.ok(planUpgradeService.submitUpgradeRequest(request));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<PaymentResponseDTO> checkoutUpgrade(@PathVariable Long id) {
        log.info("Plan upgrade checkout requested for upgradeId={}", id);
        return ResponseEntity.ok(paymentService.checkoutUpgrade(id));
    }
}
