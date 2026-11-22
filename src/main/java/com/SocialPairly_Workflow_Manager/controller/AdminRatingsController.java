package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AppRatingDto;
import com.SocialPairly_Workflow_Manager.service.AppRatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Ratings console for Super Admin / Admin / Customer Support (RATINGS RBAC key).
 */
@RestController
@RequestMapping("/api/admin/ratings")
public class AdminRatingsController {

    private final AppRatingService appRatingService;

    public AdminRatingsController(AppRatingService appRatingService) {
        this.appRatingService = appRatingService;
    }

    @GetMapping
    public ResponseEntity<List<AppRatingDto>> list() {
        return ResponseEntity.ok(appRatingService.listAllRatings());
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok(appRatingService.summary());
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of(
                "unreadCount", appRatingService.countMyUnreadRatingNotifications()));
    }

    @PostMapping("/notifications/mark-read")
    public ResponseEntity<Map<String, Long>> markRead() {
        return ResponseEntity.ok(Map.of(
                "markedRead", appRatingService.markMyRatingNotificationsRead()));
    }
}
