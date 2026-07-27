package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.PlanDto;
import com.SocialPairly_Workflow_Manager.dto.PlanRequest;
import com.SocialPairly_Workflow_Manager.service.PlanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private static final Logger log = LoggerFactory.getLogger(PlanController.class);
    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<List<PlanDto>> getAllActivePlans() {
        log.debug("Fetching active plans from database");
        return ResponseEntity.ok(planService.getActivePlans());
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<PlanDto>> getAllPlansForAdmin() {
        log.debug("Fetching all plans (active and inactive) for admin");
        return ResponseEntity.ok(planService.getAllPlans());
    }

    @PostMapping("/admin")
    public ResponseEntity<PlanDto> createPlan(@Valid @RequestBody PlanRequest request) {
        log.info("Admin creating plan: {}", request.planName());
        PlanDto createdPlan = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlan);
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<PlanDto> updatePlan(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        log.info("Admin updating plan id={}", id);
        PlanDto updatedPlan = planService.updatePlan(id, request);
        return ResponseEntity.ok(updatedPlan);
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Map<String, String>> deletePlan(@PathVariable Long id) {
        log.info("Admin deleting plan id={}", id);
        planService.deletePlan(id);
        return ResponseEntity.ok(Map.of("message", "Plan deleted successfully"));
    }
}
