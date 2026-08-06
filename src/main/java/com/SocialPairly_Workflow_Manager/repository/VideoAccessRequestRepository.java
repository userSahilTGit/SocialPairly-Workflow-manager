package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.AccessRequestStatus;
import com.SocialPairly_Workflow_Manager.entity.UserMedia;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.VideoAccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VideoAccessRequestRepository extends JpaRepository<VideoAccessRequest, Long> {
    List<VideoAccessRequest> findByMediaId(Long mediaId);
    List<VideoAccessRequest> findByRequesterId(Long requesterId);
    List<VideoAccessRequest> findByOwnerId(Long ownerId);
    List<VideoAccessRequest> findByOwnerIdAndStatus(Long ownerId, AccessRequestStatus status);
    Optional<VideoAccessRequest> findByMediaIdAndRequesterId(Long mediaId, Long requesterId);
    List<VideoAccessRequest> findByMediaIdAndStatus(Long mediaId, AccessRequestStatus status);
    boolean existsByMediaIdAndRequesterIdAndStatus(Long mediaId, Long requesterId, AccessRequestStatus status);
}
