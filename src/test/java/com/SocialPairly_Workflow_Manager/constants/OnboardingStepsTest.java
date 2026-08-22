package com.SocialPairly_Workflow_Manager.constants;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OnboardingStepsTest {

    @Test
    void completedAtShortCircuitsTrue() {
        assertTrue(OnboardingSteps.isIdentityPage1Complete("anything", LocalDateTime.now()));
    }

    @Test
    void stepFlagsWithoutCompletedAt() {
        assertTrue(OnboardingSteps.isIdentityPage1Complete(OnboardingSteps.IDENTITY_COMPLETED, null));
        assertTrue(OnboardingSteps.isIdentityPage1Complete(OnboardingSteps.PERSONALITY_IN_PROGRESS, null));
        assertTrue(OnboardingSteps.isIdentityPage1Complete(OnboardingSteps.PERSONALITY_COMPLETED, null));
        assertFalse(OnboardingSteps.isIdentityPage1Complete(OnboardingSteps.IDENTITY_IN_PROGRESS, null));
        assertFalse(OnboardingSteps.isIdentityPage1Complete(null, null));
    }
}
