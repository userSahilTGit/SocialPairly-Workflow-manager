package com.SocialPairly_Workflow_Manager.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeRoleCodesTest {

    @Test
    void validatesAndNormalizesRoleCodes() {
        assertTrue(EmployeeRoleCodes.isValid("admin"));
        assertTrue(EmployeeRoleCodes.isValid("SUPER_ADMIN"));
        assertFalse(EmployeeRoleCodes.isValid("UNKNOWN"));
        assertFalse(EmployeeRoleCodes.isValid(null));
        assertEquals("FINANCE", EmployeeRoleCodes.normalize(" finance "));
        assertNull(EmployeeRoleCodes.normalize(null));
        assertTrue(EmployeeRoleCodes.all().contains(EmployeeRoleCodes.QC_LEGAL));
    }

    @Test
    void detectsSocialPairlyDomain() {
        assertTrue(EmployeeRoleCodes.isSocialPairlyDomain("Sahil.T@SocialPairly.com"));
        assertFalse(EmployeeRoleCodes.isSocialPairlyDomain("user@gmail.com"));
        assertFalse(EmployeeRoleCodes.isSocialPairlyDomain(null));
        assertFalse(EmployeeRoleCodes.isSocialPairlyDomain(""));
        assertFalse(EmployeeRoleCodes.isSocialPairlyDomain("no-at-sign"));
        assertFalse(EmployeeRoleCodes.isSocialPairlyDomain("trailing@"));
    }
}
