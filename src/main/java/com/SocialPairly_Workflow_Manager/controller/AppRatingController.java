package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AppRatingDto;
import com.SocialPairly_Workflow_Manager.dto.CreateAppRatingRequest;
import com.SocialPairly_Workflow_Manager.service.AppRatingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ratings")
public class AppRatingController {

    private static final Logger log = LoggerFactory.getLogger(AppRatingController.class);

    private final AppRatingService appRatingService;

    public AppRatingController(AppRatingService appRatingService) {
        this.appRatingService = appRatingService;
    }

    @PostMapping
    public ResponseEntity<AppRatingDto> submit(@Valid @RequestBody CreateAppRatingRequest request) {
        log.info("Member submitting app rating stars={}", request.stars());
        return ResponseEntity.ok(appRatingService.submitRating(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AppRatingDto> myLatest() {
        AppRatingDto latest = appRatingService.getMyLatestRating();
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }
}
