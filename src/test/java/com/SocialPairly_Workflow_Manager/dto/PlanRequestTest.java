package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlanRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should pass validation when all fields are valid")
    void shouldPassValidationWhenFieldsAreValid() {
        PlanRequest request = new PlanRequest(
                "Pro Plan",
                "MONTHLY",
                30,
                new BigDecimal("29.99"),
                "5000",
                "UNLIMITED",
                "PRO_BADGE",
                "Full access to pro features",
                true,
                false
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when required fields are missing")
    void shouldFailValidationWhenRequiredFieldsAreMissing() {
        // Arrange: Pass nulls for all fields in the record
        PlanRequest request = new PlanRequest(
                null, null, null, null, null, null, null, null, null, null
        );

        // Act
        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);

        // Assert: 6 required fields produce exactly 6 constraint violations
        assertThat(violations).hasSize(6);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Plan name is required",
                        "Plan type is required",
                        "Duration days is required",
                        "Amount is required",
                        "Description is required",
                        "Active status is required"
                );
    }

    @Test
    @DisplayName("Should fail validation when planName exceeds maximum length")
    void shouldFailValidationWhenPlanNameIsTooLong() {
        String longPlanName = "A".repeat(51);
        PlanRequest request = new PlanRequest(
                longPlanName,
                "MONTHLY",
                30,
                new BigDecimal("29.99"),
                "5000",
                "UNLIMITED",
                "PRO_BADGE",
                "Full access to pro features",
                true,
                false
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Plan name must be between 1 and 50 characters");
    }

    @Test
    @DisplayName("Should fail validation when durationDays or amount are non-positive")
    void shouldFailValidationWhenNumericFieldsAreInvalid() {
        PlanRequest request = new PlanRequest(
                "Pro Plan",
                "MONTHLY",
                0, // Invalid: Must be min 1
                new BigDecimal("-5.00"), // Invalid: Must be positive
                "5000",
                "UNLIMITED",
                "PRO_BADGE",
                "Full access to pro features",
                true,
                false
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsAnyOf(
                        "Duration days must be at least 1",
                        "Amount must be greater than zero"
                );
    }
}