package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.constants.CallbackTimeSlots;
import com.SocialPairly_Workflow_Manager.dto.CreateSupportCallbackRequest;
import com.SocialPairly_Workflow_Manager.dto.SupportCallbackRequestDto;
import com.SocialPairly_Workflow_Manager.service.SupportCallbackService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportCallbackController {

    private static final Logger log = LoggerFactory.getLogger(SupportCallbackController.class);

    private final SupportCallbackService supportCallbackService;

    public SupportCallbackController(SupportCallbackService supportCallbackService) {
        this.supportCallbackService = supportCallbackService;
    }

    @PostMapping("/callback")
    public ResponseEntity<SupportCallbackRequestDto> requestCallback(
            @Valid @RequestBody(required = false) CreateSupportCallbackRequest request
    ) {
        log.info("Premium support callback requested");
        return ResponseEntity.ok(supportCallbackService.requestCallback(
                request == null ? new CreateSupportCallbackRequest(null) : request
        ));
    }

    @GetMapping("/callback/active")
    public ResponseEntity<SupportCallbackRequestDto> activeCallback() {
        SupportCallbackRequestDto active = supportCallbackService.getMyActiveCallback();
        if (active == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(active);
    }

    @GetMapping("/callback/slots")
    public ResponseEntity<List<Map<String, String>>> listCallbackSlots() {
        return ResponseEntity.ok(CallbackTimeSlots.catalog());
    }
}
