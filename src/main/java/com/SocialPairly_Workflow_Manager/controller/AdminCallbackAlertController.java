package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.StaffInboxNotificationDto;
import com.SocialPairly_Workflow_Manager.service.SupportCallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Callback alerts for Super Admin / Admin main dashboard (DASHBOARD RBAC key).
 * Full queue management stays on {@code /api/admin/customer-support/**}.
 */
@RestController
@RequestMapping("/api/admin/callback-alerts")
public class AdminCallbackAlertController {

    private final SupportCallbackService supportCallbackService;

    public AdminCallbackAlertController(SupportCallbackService supportCallbackService) {
        this.supportCallbackService = supportCallbackService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> summary() {
        return ResponseEntity.ok(Map.of(
                "pendingCount", supportCallbackService.countPending(),
                "unreadNotifications", supportCallbackService.countMyUnreadNotifications()
        ));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<StaffInboxNotificationDto>> notifications() {
        return ResponseEntity.ok(supportCallbackService.listMyNotifications());
    }
}
