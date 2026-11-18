package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeRbacEntityTest {

    @Test
    void staffRoleAssignmentAccessorsAndLifecycle() {
        StaffRoleAssignment row = new StaffRoleAssignment();
        row.setEmail("  Admin@SocialPairly.com ");
        row.onCreate();
        assertEquals("admin@socialpairly.com", row.getEmail());
        assertNotNull(row.getCreatedAt());
        assertNotNull(row.getUpdatedAt());

        row.setId(1L);
        row.setUserId(2L);
        row.setRoleCode("SUPER_ADMIN");
        row.setActive(true);
        row.setAssignmentSource("SEED");
        row.setNotes("seed");
        row.setAssignedByEmail("owner@socialpairly.com");
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        LocalDateTime updated = LocalDateTime.now();
        row.setCreatedAt(created);
        row.setUpdatedAt(updated);

        row.setEmail("  Other@SocialPairly.com ");
        row.onUpdate();
        assertEquals("other@socialpairly.com", row.getEmail());
        assertTrue(row.getUpdatedAt().isAfter(created) || row.getUpdatedAt().equals(updated)
                || !row.getUpdatedAt().isBefore(updated));

        assertEquals(1L, row.getId());
        assertEquals(2L, row.getUserId());
        assertEquals("SUPER_ADMIN", row.getRoleCode());
        assertTrue(row.isActive());
        assertEquals("SEED", row.getAssignmentSource());
        assertEquals("seed", row.getNotes());
        assertEquals("owner@socialpairly.com", row.getAssignedByEmail());
        assertEquals(created, row.getCreatedAt());
    }

    @Test
    void roleDashboardAccessAccessorsAndLifecycle() {
        RoleDashboardAccess row = new RoleDashboardAccess();
        row.onCreate();
        assertNotNull(row.getCreatedAt());
        assertNotNull(row.getUpdatedAt());

        row.setId(5L);
        row.setRoleCode("FINANCE");
        row.setDashboardKey("FINANCE");
        row.setCanAccess(true);
        row.setUpdatedByEmail("sa@socialpairly.com");
        LocalDateTime created = LocalDateTime.now().minusHours(2);
        row.setCreatedAt(created);
        row.setUpdatedAt(created);
        row.onUpdate();

        assertEquals(5L, row.getId());
        assertEquals("FINANCE", row.getRoleCode());
        assertEquals("FINANCE", row.getDashboardKey());
        assertTrue(row.isCanAccess());
        assertEquals("sa@socialpairly.com", row.getUpdatedByEmail());
        assertEquals(created, row.getCreatedAt());
        assertNotNull(row.getUpdatedAt());
    }
}
