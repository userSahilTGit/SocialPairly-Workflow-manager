package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.RoleDashboardAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleDashboardAccessRepository extends JpaRepository<RoleDashboardAccess, Long> {

    List<RoleDashboardAccess> findByRoleCodeAndCanAccessTrue(String roleCode);

    List<RoleDashboardAccess> findAllByOrderByRoleCodeAscDashboardKeyAsc();

    Optional<RoleDashboardAccess> findByRoleCodeAndDashboardKey(String roleCode, String dashboardKey);

    List<RoleDashboardAccess> findByRoleCode(String roleCode);
}
