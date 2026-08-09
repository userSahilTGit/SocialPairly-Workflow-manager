package com.SocialPairly_Workflow_Manager.constants;

public final class OnboardingSteps {

    private OnboardingSteps() {}

    public static final String STEP_1_ACCOUNT = "STEP_1_ACCOUNT";
    public static final String STEP_2_VERIFY_EMAIL = "STEP_2_VERIFY_EMAIL";
    public static final String STEP_3_VERIFY_PHONE = "STEP_3_VERIFY_PHONE";
    public static final String STEP_4_COMPLETED = "STEP_4_COMPLETED";
    public static final String IDENTITY_IN_PROGRESS = "IDENTITY_IN_PROGRESS";
    public static final String IDENTITY_COMPLETED = "IDENTITY_COMPLETED";
    public static final String PERSONALITY_IN_PROGRESS = "PERSONALITY_IN_PROGRESS";
    public static final String PERSONALITY_COMPLETED = "PERSONALITY_COMPLETED";

    public static final String BACKGROUND_CONSENT_DOCUMENT_VERSION = "BG_SCREEN_V1";

    public static boolean isIdentityPage1Complete(String step, java.time.LocalDateTime completedAt) {
        if (completedAt != null) {
            return true;
        }
        return IDENTITY_COMPLETED.equals(step)
                || PERSONALITY_IN_PROGRESS.equals(step)
                || PERSONALITY_COMPLETED.equals(step);
    }
}
