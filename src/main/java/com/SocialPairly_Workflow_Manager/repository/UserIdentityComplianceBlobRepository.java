package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.UserIdentityComplianceBlob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserIdentityComplianceBlobRepository extends JpaRepository<UserIdentityComplianceBlob, Long> {

    List<UserIdentityComplianceBlob> findByComplianceId(Long complianceId);
}
