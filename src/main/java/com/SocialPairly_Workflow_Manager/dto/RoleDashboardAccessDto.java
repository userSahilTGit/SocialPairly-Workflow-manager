package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.RoleDashboardAccess;

import java.time.LocalDateTime;

public record RoleDashboardAccessDto(
        Long id,
        String roleCode,
        String dashboardKey,
        boolean canAccess,
        String updatedByEmail,
        LocalDateTime updatedAt
) {
    public static RoleDashboardAccessDto from(RoleDashboardAccess row) {
        return new RoleDashboardAccessDto(
                row.getId(),
                row.getRoleCode(),
                row.getDashboardKey(),
                row.isCanAccess(),
                row.getUpdatedByEmail(),
                row.getUpdatedAt()
        );
    }
}
