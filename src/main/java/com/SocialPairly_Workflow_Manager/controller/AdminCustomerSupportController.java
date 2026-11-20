package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.StaffInboxNotificationDto;
import com.SocialPairly_Workflow_Manager.dto.SupportCallbackRequestDto;
import com.SocialPairly_Workflow_Manager.dto.UpdateSupportCallbackStatusRequest;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackStatus;
import com.SocialPairly_Workflow_Manager.service.SupportCallbackService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/customer-support")
public class AdminCustomerSupportController {

    private static final Logger log = LoggerFactory.getLogger(AdminCustomerSupportController.class);

    private final SupportCallbackService supportCallbackService;

    public AdminCustomerSupportController(SupportCallbackService supportCallbackService) {
        this.supportCallbackService = supportCallbackService;
    }

    @GetMapping("/callbacks")
    public ResponseEntity<List<SupportCallbackRequestDto>> listCallbacks(
            @RequestParam(value = "status", required = false) String status
    ) {
        SupportCallbackStatus parsed = parseStatus(status);
        log.info("Admin listing support callbacks status={}", parsed);
        return ResponseEntity.ok(supportCallbackService.listCallbacks(parsed));
    }

    @GetMapping("/callbacks/pending-count")
    public ResponseEntity<Map<String, Long>> pendingCount() {
        return ResponseEntity.ok(Map.of("pendingCount", supportCallbackService.countPending()));
    }

    @PatchMapping("/callbacks/{id}")
    public ResponseEntity<SupportCallbackRequestDto> updateCallback(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupportCallbackStatusRequest request
    ) {
        log.info("Admin updating support callback id={} status={}", id, request.status());
        return ResponseEntity.ok(supportCallbackService.updateStatus(id, request));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<StaffInboxNotificationDto>> listNotifications() {
        return ResponseEntity.ok(supportCallbackService.listMyNotifications());
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", supportCallbackService.countMyUnreadNotifications()));
    }
    private static SupportCallbackStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return SupportCallbackStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
