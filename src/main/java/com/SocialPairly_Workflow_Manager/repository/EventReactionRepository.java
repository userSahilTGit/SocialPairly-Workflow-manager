package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.EventReaction;
import com.SocialPairly_Workflow_Manager.entity.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventReactionRepository extends JpaRepository<EventReaction, Long> {

    List<EventReaction> findByEventIdAndFromUserId(Long eventId, Long fromUserId);

    boolean existsByEventIdAndFromUserIdAndToUserIdAndReactionType(
            Long eventId, Long fromUserId, Long toUserId, ReactionType reactionType
    );
}
