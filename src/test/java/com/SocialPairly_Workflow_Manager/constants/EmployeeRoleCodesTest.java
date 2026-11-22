package com.SocialPairly_Workflow_Manager.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeRoleCodesTest {

    @Test
    void supportOpsRolesAreStrictlySuperAdminAdminAndCustomerSupport() {
        assertTrue(EmployeeRoleCodes.isSupportOpsRole(EmployeeRoleCodes.SUPER_ADMIN));
        assertTrue(EmployeeRoleCodes.isSupportOpsRole("admin"));
        assertTrue(EmployeeRoleCodes.isSupportOpsRole(EmployeeRoleCodes.CUSTOMER_SUPPORT));
        assertFalse(EmployeeRoleCodes.isSupportOpsRole(EmployeeRoleCodes.FINANCE));
        assertFalse(EmployeeRoleCodes.isSupportOpsRole(EmployeeRoleCodes.TESTER));
        assertFalse(EmployeeRoleCodes.isSupportOpsRole(EmployeeRoleCodes.EVENT_TEAM));
        assertFalse(EmployeeRoleCodes.isSupportOpsRole(null));
        assertEquals(3, EmployeeRoleCodes.supportOpsRoles().size());
    }
}
