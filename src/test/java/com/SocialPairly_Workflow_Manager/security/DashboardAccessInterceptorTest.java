package com.SocialPairly_Workflow_Manager.security;

import com.SocialPairly_Workflow_Manager.constants.DashboardKeys;
import com.SocialPairly_Workflow_Manager.service.EmployeeAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardAccessInterceptorTest {

    @Mock
    private EmployeeAccessService employeeAccessService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private DashboardAccessInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new DashboardAccessInterceptor(employeeAccessService);
    }

    @Test
    void resolveDashboardKeyMapsKnownPaths() {
        assertEquals(DashboardKeys.ROLE_ACCESS, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/access/employees"));
        assertEquals(DashboardKeys.CUSTOMER_SUPPORT, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/customer-support"));
        assertEquals(DashboardKeys.CUSTOMER_SUPPORT, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/callback-alerts/summary"));
        assertEquals(DashboardKeys.CUSTOMER_SUPPORT, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/callback-alerts/callbacks"));
        assertEquals(DashboardKeys.RATINGS, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/ratings"));
        assertEquals(DashboardKeys.QUESTIONS, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/questions"));
        assertEquals(DashboardKeys.MODERATION, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/media/pending"));
        assertEquals(DashboardKeys.IDENTITY_DOCS, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/identity-documents"));
        assertEquals(DashboardKeys.EVENTS, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/events"));
        assertEquals(DashboardKeys.FINANCE, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/refunds/1"));
        assertEquals(DashboardKeys.FINANCE, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/payments"));
        assertEquals(DashboardKeys.FINANCE, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/upgrades"));
        assertEquals(DashboardKeys.PLANS, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/subscriptions"));
        assertEquals(DashboardKeys.PLANS, DashboardAccessInterceptor.resolveDashboardKey("/api/plans/admin/all"));
        assertEquals(DashboardKeys.DASHBOARD, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/stats"));
        assertEquals(DashboardKeys.DASHBOARD, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/users"));
        assertEquals(DashboardKeys.DASHBOARD, DashboardAccessInterceptor.resolveDashboardKey("/api/admin"));
        assertEquals(DashboardKeys.DASHBOARD, DashboardAccessInterceptor.resolveDashboardKey("/api/admin/unknown"));
        assertNull(DashboardAccessInterceptor.resolveDashboardKey("/api/users/me"));
        assertNull(DashboardAccessInterceptor.resolveDashboardKey(null));
    }

    @Test
    void preHandleSkipsNonAdminPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/me");
        when(request.getContextPath()).thenReturn("");

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verifyNoInteractions(employeeAccessService);
    }

    @Test
    void preHandleEnforcesDashboardForAdminPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/context/api/admin/payments");
        when(request.getContextPath()).thenReturn("/context");

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(employeeAccessService).requireDashboard(DashboardKeys.FINANCE);
    }

    @Test
    void preHandleHandlesNullPath() throws Exception {
        when(request.getRequestURI()).thenReturn(null);
        assertTrue(interceptor.preHandle(request, response, new Object()));
        verifyNoInteractions(employeeAccessService);
    }
}
