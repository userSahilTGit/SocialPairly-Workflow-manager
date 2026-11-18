package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AssignEmployeeRoleRequest;
import com.SocialPairly_Workflow_Manager.dto.EmployeeRoleAssignmentDto;
import com.SocialPairly_Workflow_Manager.dto.RoleDashboardAccessDto;
import com.SocialPairly_Workflow_Manager.dto.UpdateEmployeeRoleRequest;
import com.SocialPairly_Workflow_Manager.dto.UpdateRoleDashboardAccessRequest;
import com.SocialPairly_Workflow_Manager.service.EmployeeAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeAccessControllerTest {

    @Mock
    private EmployeeAccessService employeeAccessService;

    @InjectMocks
    private EmployeeAccessController controller;

    @Test
    void catalogDelegates() {
        when(employeeAccessService.catalog()).thenReturn(Map.of("roles", List.of("ADMIN")));
        ResponseEntity<Map<String, Object>> response = controller.catalog();
        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("ADMIN"), response.getBody().get("roles"));
    }

    @Test
    void listAndMutateEmployees() {
        EmployeeRoleAssignmentDto dto = new EmployeeRoleAssignmentDto(
                1L, "a@b.com", 2L, "ADMIN", true, "MANUAL", null, "sa@x.com",
                "A", "B", LocalDateTime.now(), LocalDateTime.now());
        when(employeeAccessService.listAssignments()).thenReturn(List.of(dto));
        when(employeeAccessService.assignRole(any())).thenReturn(dto);
        when(employeeAccessService.updateAssignment(eq(1L), any())).thenReturn(dto);

        assertEquals(1, controller.listEmployees().getBody().size());
        assertSame(dto, controller.assignEmployee(new AssignEmployeeRoleRequest("a@b.com", "ADMIN", null)).getBody());
        assertSame(dto, controller.updateEmployee(1L, new UpdateEmployeeRoleRequest("FINANCE", true, null)).getBody());
    }

    @Test
    void listAndUpdateDashboards() {
        RoleDashboardAccessDto dto = new RoleDashboardAccessDto(
                1L, "ADMIN", "DASHBOARD", true, "sa@x.com", LocalDateTime.now());
        when(employeeAccessService.listDashboardAccess()).thenReturn(List.of(dto));
        when(employeeAccessService.updateDashboardAccess(any())).thenReturn(dto);

        assertEquals(1, controller.listDashboardAccess().getBody().size());
        assertSame(dto, controller.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest("ADMIN", "DASHBOARD", true)).getBody());
    }
}
