package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AssignEmployeeRoleRequest;
import com.SocialPairly_Workflow_Manager.dto.EmployeeRoleAssignmentDto;
import com.SocialPairly_Workflow_Manager.dto.RoleDashboardAccessDto;
import com.SocialPairly_Workflow_Manager.dto.UpdateEmployeeRoleRequest;
import com.SocialPairly_Workflow_Manager.dto.UpdateRoleDashboardAccessRequest;
import com.SocialPairly_Workflow_Manager.service.EmployeeAccessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/access")
public class EmployeeAccessController {

    private final EmployeeAccessService employeeAccessService;

    public EmployeeAccessController(EmployeeAccessService employeeAccessService) {
        this.employeeAccessService = employeeAccessService;
    }

    @GetMapping("/catalog")
    public ResponseEntity<Map<String, Object>> catalog() {
        return ResponseEntity.ok(employeeAccessService.catalog());
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeRoleAssignmentDto>> listEmployees() {
        return ResponseEntity.ok(employeeAccessService.listAssignments());
    }

    @PostMapping("/employees")
    public ResponseEntity<EmployeeRoleAssignmentDto> assignEmployee(@Valid @RequestBody AssignEmployeeRoleRequest request) {
        return ResponseEntity.ok(employeeAccessService.assignRole(request));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeRoleAssignmentDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRoleRequest request) {
        return ResponseEntity.ok(employeeAccessService.updateAssignment(id, request));
    }

    @GetMapping("/dashboards")
    public ResponseEntity<List<RoleDashboardAccessDto>> listDashboardAccess() {
        return ResponseEntity.ok(employeeAccessService.listDashboardAccess());
    }

    @PutMapping("/dashboards")
    public ResponseEntity<RoleDashboardAccessDto> updateDashboardAccess(
            @Valid @RequestBody UpdateRoleDashboardAccessRequest request) {
        return ResponseEntity.ok(employeeAccessService.updateDashboardAccess(request));
    }
}
