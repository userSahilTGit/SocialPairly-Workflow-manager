package com.SocialPairly_Workflow_Manager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocialPairlyWorkflowMangerApplicationProfileTest {

    private static final String APPLIED = "socialpairly.env-profile.applied";
    private static final String LEGACY = "socialpairly.prod-flag.applied";

    @AfterEach
    void clearProfileProperties() {
        System.clearProperty("spring.profiles.active");
        System.clearProperty(APPLIED);
        System.clearProperty(LEGACY);
        System.clearProperty("app.prod-flag");
        System.clearProperty("app.preprod-flag");
    }

    @Test
    void prodArgumentReplacesTheDevProfile() {
        System.setProperty(LEGACY, "1");

        SocialPairlyWorkflowMangerApplication.applyLaunchProfile(new String[]{"--app.prod-flag=true"});

        assertEquals("prod", System.getProperty("spring.profiles.active"));
        assertEquals("prod", System.getProperty(APPLIED));
        assertNull(System.getProperty(LEGACY));
    }

    @Test
    void preprodArgumentIsAppliedWhenProdIsOff() {
        SocialPairlyWorkflowMangerApplication.applyLaunchProfile(
                new String[]{"--app.prod-flag=false", "--app.preprod-flag=yes"});

        assertEquals("preprod", System.getProperty("spring.profiles.active"));
        assertEquals("preprod", System.getProperty(APPLIED));
    }

    @Test
    void turningFlagsOffClearsAProfileThisProcessApplied() {
        System.setProperty("spring.profiles.active", "prod");
        System.setProperty(APPLIED, "prod");

        SocialPairlyWorkflowMangerApplication.applyLaunchProfile(
                new String[]{"--app.prod-flag=false", "--app.preprod-flag=false"});

        assertNull(System.getProperty("spring.profiles.active"));
        assertNull(System.getProperty(APPLIED));
    }

    @Test
    void legacyMarkerIsClearedWhenFlagsAreOff() {
        System.setProperty("spring.profiles.active", "prod");
        System.setProperty(LEGACY, "1");

        SocialPairlyWorkflowMangerApplication.applyLaunchProfile(new String[]{"--app.prod-flag=0"});

        assertNull(System.getProperty("spring.profiles.active"));
        assertNull(System.getProperty(LEGACY));
    }

    @Test
    void explicitLaunchProfileIsLeftAloneWhenThisClassDidNotApplyIt() {
        System.setProperty("spring.profiles.active", "local");

        SocialPairlyWorkflowMangerApplication.applyLaunchProfile(new String[]{"--app.prod-flag=false"});

        assertEquals("local", System.getProperty("spring.profiles.active"));
        assertNull(System.getProperty(APPLIED));
    }

    @Test
    void logActiveConfigNamesTheFileThatOverridesTheBase() {
        Environment prod = mock(Environment.class);
        when(prod.getActiveProfiles()).thenReturn(new String[]{"prod"});
        Environment preprod = mock(Environment.class);
        when(preprod.getActiveProfiles()).thenReturn(new String[]{"preprod", "extra"});
        Environment dev = mock(Environment.class);
        when(dev.getActiveProfiles()).thenReturn(new String[]{});

        SocialPairlyWorkflowMangerApplication.logActiveConfig(prod);
        SocialPairlyWorkflowMangerApplication.logActiveConfig(preprod);
        SocialPairlyWorkflowMangerApplication.logActiveConfig(dev);
    }
}
