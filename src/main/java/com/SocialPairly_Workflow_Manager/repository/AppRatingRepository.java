package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.AppRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppRatingRepository extends JpaRepository<AppRating, Long> {

    List<AppRating> findAllByOrderByCreatedAtDesc();

    Optional<AppRating> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);

    long countByStars(Integer stars);
}
