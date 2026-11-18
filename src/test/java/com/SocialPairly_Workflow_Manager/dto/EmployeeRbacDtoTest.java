package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.RoleDashboardAccess;
import com.SocialPairly_Workflow_Manager.entity.StaffRoleAssignment;
import com.SocialPairly_Workflow_Manager.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeRbacDtoTest {

    @Test
    void employeeRoleAssignmentDtoFromEntity() {
        StaffRoleAssignment row = new StaffRoleAssignment();
        row.setId(1L);
        row.setEmail("a@b.com");
        row.setUserId(2L);
        row.setRoleCode("ADMIN");
        row.setActive(true);
        row.setAssignmentSource("MANUAL");
        row.setNotes("n");
        row.setAssignedByEmail("sa@x.com");
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());

        EmployeeRoleAssignmentDto bare = EmployeeRoleAssignmentDto.from(row);
        assertEquals("a@b.com", bare.email());
        assertNull(bare.firstName());

        EmployeeRoleAssignmentDto named = EmployeeRoleAssignmentDto.from(row, "Ada", "Lovelace");
        assertEquals("Ada", named.firstName());
        assertEquals("Lovelace", named.lastName());
    }

    @Test
    void roleDashboardAccessDtoFromEntity() {
        RoleDashboardAccess row = new RoleDashboardAccess();
        row.setId(3L);
        row.setRoleCode("ADMIN");
        row.setDashboardKey("DASHBOARD");
        row.setCanAccess(true);
        row.setUpdatedByEmail("sa@x.com");
        row.setUpdatedAt(LocalDateTime.now());

        RoleDashboardAccessDto dto = RoleDashboardAccessDto.from(row);
        assertEquals("ADMIN", dto.roleCode());
        assertEquals("DASHBOARD", dto.dashboardKey());
        assertTrue(dto.canAccess());
    }

    @Test
    void userDtoWithEmployeeAccessPreservesFields() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Ada");
        user.setEmail("ada@example.com");
        user.setRole(Role.ADMIN);
        user.setUserTokens(10);

        UserDto base = UserDto.from(user);
        UserDto enriched = base.withEmployeeAccess("SUPER_ADMIN", List.of("DASHBOARD", "ROLE_ACCESS"));
        assertEquals("SUPER_ADMIN", enriched.employeeRole());
        assertEquals(List.of("DASHBOARD", "ROLE_ACCESS"), enriched.allowedDashboards());
        assertEquals(base.email(), enriched.email());
        assertEquals(base.role(), enriched.role());

        UserDto subscribed = UserDto.from(user, true).withEmployeeAccess("ADMIN", null);
        assertEquals("Subscribed", subscribed.subscriptionDetails());
        assertTrue(subscribed.allowedDashboards().isEmpty());
    }

    @Test
    void requestRecordsHoldValues() {
        AssignEmployeeRoleRequest assign = new AssignEmployeeRoleRequest("a@b.com", "ADMIN", "note");
        assertEquals("ADMIN", assign.roleCode());
        UpdateEmployeeRoleRequest update = new UpdateEmployeeRoleRequest("FINANCE", false, "x");
        assertFalse(update.active());
        UpdateRoleDashboardAccessRequest dash = new UpdateRoleDashboardAccessRequest("ADMIN", "DASHBOARD", true);
        assertTrue(dash.canAccess());
    }
}
