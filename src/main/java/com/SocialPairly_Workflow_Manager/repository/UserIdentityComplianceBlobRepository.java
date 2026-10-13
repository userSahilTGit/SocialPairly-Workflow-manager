package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.UserIdentityComplianceBlob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface UserIdentityComplianceBlobRepository extends JpaRepository<UserIdentityComplianceBlob, Long> {

    List<UserIdentityComplianceBlob> findByComplianceId(Long complianceId);

    /**
     * Admin catalog of ID images without loading {@code storage_blob} LONGBLOB bytes.
     */
    @Query("""
            SELECT b FROM UserIdentityComplianceBlob b
            JOIN FETCH b.compliance c
            JOIN FETCH c.user u
            WHERE b.docPurpose IN :purposes
            ORDER BY u.id ASC, b.createdAt DESC
            """)
    List<UserIdentityComplianceBlob> findAllForAdminByPurposes(Collection<String> purposes);
}
