package com.SocialPairly_Workflow_Manager.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashboardKeysTest {

    @Test
    void validatesAndNormalizesDashboardKeys() {
        assertTrue(DashboardKeys.isValid("dashboard"));
        assertTrue(DashboardKeys.isValid(DashboardKeys.ROLE_ACCESS));
        assertFalse(DashboardKeys.isValid("UNKNOWN"));
        assertFalse(DashboardKeys.isValid(null));
        assertEquals("FINANCE", DashboardKeys.normalize(" finance "));
        assertNull(DashboardKeys.normalize(null));
        assertTrue(DashboardKeys.all().contains(DashboardKeys.CUSTOMER_SUPPORT));
        assertTrue(DashboardKeys.all().contains(DashboardKeys.RATINGS));
        assertTrue(DashboardKeys.isValid(DashboardKeys.RATINGS));
    }
}
