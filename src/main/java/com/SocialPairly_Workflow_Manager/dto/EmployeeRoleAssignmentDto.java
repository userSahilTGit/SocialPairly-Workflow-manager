package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.StaffRoleAssignment;

import java.time.LocalDateTime;

public record EmployeeRoleAssignmentDto(
        Long id,
        String email,
        Long userId,
        String roleCode,
        boolean active,
        String assignmentSource,
        String notes,
        String assignedByEmail,
        String firstName,
        String lastName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EmployeeRoleAssignmentDto from(StaffRoleAssignment row) {
        return from(row, null, null);
    }

    public static EmployeeRoleAssignmentDto from(StaffRoleAssignment row, String firstName, String lastName) {
        return new EmployeeRoleAssignmentDto(
                row.getId(),
                row.getEmail(),
                row.getUserId(),
                row.getRoleCode(),
                row.isActive(),
                row.getAssignmentSource(),
                row.getNotes(),
                row.getAssignedByEmail(),
                firstName,
                lastName,
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }
}
