package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {

    List<UserAnswer> findByUserId(Long userId);

    Optional<UserAnswer> findByUserIdAndQuestionId(Long userId, Long questionId);

    long countByQuestionId(Long questionId);
}