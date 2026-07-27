package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.Plan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PlanRepositoryTest {

    @Autowired
    private PlanRepository planRepository;

    @Test
    void testFindByIsActiveTrueOrderByDurationDaysAsc() {
        // Create active plans
        Plan plan1 = new Plan();
        plan1.setPlanName("Basic Plan");
        plan1.setPlanType("MONTHLY");
        plan1.setDurationDays(30);
        plan1.setAmount(new BigDecimal("10.00"));
        plan1.setActive(true);
        planRepository.save(plan1);

        Plan plan2 = new Plan();
        plan2.setPlanName("Premium Plan");
        plan2.setPlanType("YEARLY");
        plan2.setDurationDays(365);
        plan2.setAmount(new BigDecimal("99.99"));
        plan2.setActive(true);
        planRepository.save(plan2);

        Plan plan3 = new Plan();
        plan3.setPlanName("Free Plan");
        plan3.setPlanType("WEEKLY");
        plan3.setDurationDays(7);
        plan3.setAmount(new BigDecimal("0.00"));
        plan3.setActive(true);
        planRepository.save(plan3);

        // Create inactive plan
        Plan inactivePlan = new Plan();
        inactivePlan.setPlanName("Inactive Plan");
        inactivePlan.setPlanType("MONTHLY");
        inactivePlan.setDurationDays(30);
        inactivePlan.setAmount(new BigDecimal("15.00"));
        inactivePlan.setActive(false);
        planRepository.save(inactivePlan);

        // Test query
        List<Plan> activePlans = planRepository.findByIsActiveTrueOrderByDurationDaysAsc();
        assertEquals(3, activePlans.size());
        assertEquals("Free Plan", activePlans.get(0).getPlanName()); // 7 days
        assertEquals("Basic Plan", activePlans.get(1).getPlanName()); // 30 days
        assertEquals("Premium Plan", activePlans.get(2).getPlanName()); // 365 days
    }

    @Test
    void testFindById() {
        Plan plan = new Plan();
        plan.setPlanName("Test Plan");
        plan.setPlanType("MONTHLY");
        plan.setDurationDays(30);
        plan.setAmount(new BigDecimal("10.00"));
        plan.setActive(true);
        plan = planRepository.save(plan);

        Optional<Plan> foundPlan = planRepository.findById(plan.getId());
        assertTrue(foundPlan.isPresent());
        assertEquals("Test Plan", foundPlan.get().getPlanName());
    }

    @Test
    void testFindAllByOrderByDurationDaysAsc() {
        // Create plans with different durations
        Plan plan1 = new Plan();
        plan1.setPlanName("Short Plan");
        plan1.setPlanType("WEEKLY");
        plan1.setDurationDays(7);
        plan1.setAmount(new BigDecimal("5.00"));
        plan1.setActive(true);
        planRepository.save(plan1);

        Plan plan2 = new Plan();
        plan2.setPlanName("Medium Plan");
        plan2.setPlanType("MONTHLY");
        plan2.setDurationDays(30);
        plan2.setAmount(new BigDecimal("15.00"));
        plan2.setActive(true);
        planRepository.save(plan2);

        Plan plan3 = new Plan();
        plan3.setPlanName("Long Plan");
        plan3.setPlanType("YEARLY");
        plan3.setDurationDays(365);
        plan3.setAmount(new BigDecimal("99.99"));
        plan3.setActive(true);
        planRepository.save(plan3);

        List<Plan> allPlans = planRepository.findAllByOrderByDurationDaysAsc();
        assertEquals(3, allPlans.size());
        assertEquals("Short Plan", allPlans.get(0).getPlanName());
        assertEquals("Medium Plan", allPlans.get(1).getPlanName());
        assertEquals("Long Plan", allPlans.get(2).getPlanName());
    }
}