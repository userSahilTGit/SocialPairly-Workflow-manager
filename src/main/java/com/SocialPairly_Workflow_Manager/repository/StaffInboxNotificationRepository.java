package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.StaffInboxNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StaffInboxNotificationRepository extends JpaRepository<StaffInboxNotification, Long> {

    List<StaffInboxNotification> findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(String recipientEmail);

    List<StaffInboxNotification> findByRecipientRoleCodeInAndReadFalseOrderByCreatedAtDesc(Collection<String> roleCodes);

    List<StaffInboxNotification> findByRecipientEmailIgnoreCaseAndReadFalseOrderByCreatedAtDesc(String recipientEmail);

    long countByRecipientEmailIgnoreCaseAndReadFalse(String recipientEmail);

    long countByRecipientEmailIgnoreCaseAndTypeAndReadFalse(String recipientEmail, String type);

    List<StaffInboxNotification> findByRecipientEmailIgnoreCaseAndTypeAndReadFalse(
            String recipientEmail, String type);

    List<StaffInboxNotification> findByRecipientEmailIgnoreCaseAndTypeAndReadFalseOrderByCreatedAtDesc(
            String recipientEmail, String type);

    List<StaffInboxNotification> findByRelatedCallbackIdAndReadFalse(Long relatedCallbackId);
}
