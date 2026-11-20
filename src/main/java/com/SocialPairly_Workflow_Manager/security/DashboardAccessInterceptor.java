package com.SocialPairly_Workflow_Manager.security;

import com.SocialPairly_Workflow_Manager.constants.DashboardKeys;
import com.SocialPairly_Workflow_Manager.service.EmployeeAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Maps admin API paths to dashboard keys and enforces employee dashboard RBAC.
 * Coarse {@code ROLE_ADMIN} is still required by SecurityConfig; this adds fine-grained checks.
 */
@Component
public class DashboardAccessInterceptor implements HandlerInterceptor {

    private final EmployeeAccessService employeeAccessService;

    public DashboardAccessInterceptor(EmployeeAccessService employeeAccessService) {
        this.employeeAccessService = employeeAccessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        // Strip context path if present
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        String dashboardKey = resolveDashboardKey(path);
        if (dashboardKey == null) {
            return true;
        }
        employeeAccessService.requireDashboard(dashboardKey);
        return true;
    }

    static String resolveDashboardKey(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("/api/admin/access")) {
            return DashboardKeys.ROLE_ACCESS;
        }
        if (path.startsWith("/api/admin/callback-alerts")) {
            // Super Admin / Admin home dashboard can surface pending callback badges.
            return DashboardKeys.DASHBOARD;
        }
        if (path.startsWith("/api/admin/customer-support")) {
            return DashboardKeys.CUSTOMER_SUPPORT;
        }
        if (path.startsWith("/api/admin/questions")) {
            return DashboardKeys.QUESTIONS;
        }
        if (path.startsWith("/api/admin/media")) {
            return DashboardKeys.MODERATION;
        }
        if (path.startsWith("/api/admin/identity-documents")) {
            return DashboardKeys.IDENTITY_DOCS;
        }
        if (path.startsWith("/api/admin/events")) {
            return DashboardKeys.EVENTS;
        }
        if (path.startsWith("/api/admin/payments")
                || path.startsWith("/api/admin/refunds")
                || path.startsWith("/api/admin/upgrades")) {
            return DashboardKeys.FINANCE;
        }
        if (path.startsWith("/api/admin/subscriptions")
                || path.startsWith("/api/plans/admin")) {
            return DashboardKeys.PLANS;
        }
        if (path.startsWith("/api/admin/stats")
                || path.startsWith("/api/admin/users")
                || path.equals("/api/admin")
                || path.equals("/api/admin/")) {
            return DashboardKeys.DASHBOARD;
        }
        // Other /api/admin/** endpoints: require at least Dashboard access
        if (path.startsWith("/api/admin/")) {
            return DashboardKeys.DASHBOARD;
        }
        return null;
    }
}
