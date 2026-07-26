package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.PlanDto;
import com.SocialPairly_Workflow_Manager.dto.PlanRequest;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);
    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<PlanDto> getActivePlans() {
        return planRepository.findByIsActiveTrueOrderByDurationDaysAsc().stream()
            .map(PlanDto::from)
            .collect(Collectors.toList());
    }

    public List<PlanDto> getAllPlans() {
        log.debug("Fetching all plans from database");
        return planRepository.findAllByOrderByDurationDaysAsc().stream()
            .map(PlanDto::from)
            .collect(Collectors.toList());
    }

    public PlanDto createPlan(PlanRequest request) {
        log.info("Creating new plan: {}", request.planName());
        Plan plan = new Plan();
        plan.setPlanName(request.planName());
        plan.setPlanType(request.planType());
        plan.setDurationDays(request.durationDays());
        plan.setAmount(request.amount());
        plan.setTokensIncluded(request.tokensIncluded());
        plan.setCommunityPosts(request.communityPosts());
        plan.setSubBadgeTag(request.subBadgeTag());
        plan.setDescription(request.description());
        plan.setActive(request.isActive() != null ? request.isActive() : true);
        plan.setFeatured(request.isFeatured() != null ? request.isFeatured() : false);

        Plan savedPlan = planRepository.save(plan);
        log.info("Plan created successfully with id: {}", savedPlan.getId());
        return PlanDto.from(savedPlan);
    }

    public PlanDto updatePlan(Long id, PlanRequest request) {
        log.info("Updating plan with id: {}", id);
        Plan plan = planRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        plan.setPlanName(request.planName());
        plan.setPlanType(request.planType());
        plan.setDurationDays(request.durationDays());
        plan.setAmount(request.amount());
        plan.setTokensIncluded(request.tokensIncluded());
        plan.setCommunityPosts(request.communityPosts());
        plan.setSubBadgeTag(request.subBadgeTag());
        plan.setDescription(request.description());
        plan.setActive(request.isActive() != null ? request.isActive() : true);
        plan.setFeatured(request.isFeatured() != null ? request.isFeatured() : false);

        Plan updatedPlan = planRepository.save(plan);
        log.info("Plan updated successfully with id: {}", id);
        return PlanDto.from(updatedPlan);
    }

    public void deletePlan(Long id) {
        log.info("Deleting plan with id: {}", id);
        Plan plan = planRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
        planRepository.delete(plan);
        log.info("Plan deleted successfully with id: {}", id);
    }
}
