package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AdminRefundDetailDto;
import com.SocialPairly_Workflow_Manager.dto.AdminStatsDto;
import com.SocialPairly_Workflow_Manager.dto.QuestionRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import com.SocialPairly_Workflow_Manager.service.AdminService;
import com.SocialPairly_Workflow_Manager.service.QuestionService;
import com.SocialPairly_Workflow_Manager.service.RefundService;
import com.SocialPairly_Workflow_Manager.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private AdminService adminService;
    @Mock private QuestionService questionService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private RefundService refundService;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(adminService, questionService, subscriptionService, refundService);
    }

    @Test
    void getStatsDelegates() {
        AdminStatsDto stats = mock(AdminStatsDto.class);
        when(adminService.getStats()).thenReturn(stats);
        assertSame(stats, controller.getStats().getBody());
    }

    @Test
    void getUsersDelegates() {
        when(adminService.listUsers()).thenReturn(List.of(mock(UserDto.class)));
        assertEquals(1, controller.getUsers().getBody().size());
    }

    @Test
    void getPaymentsDelegates() {
        when(adminService.listPayments()).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getPayments().getStatusCode());
    }

    @Test
    void getSubscriptionsDelegates() {
        when(subscriptionService.getAllSubscriptionsForAdmin()).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getSubscriptions().getStatusCode());
    }

    @Test
    void removeSubscriptionDelegates() {
        var response = controller.removeSubscription(9L);
        verify(subscriptionService).removeSubscription(9L);
        assertEquals("Subscription removed successfully", response.getBody().get("message"));
    }

    @Test
    void refundEndpointsDelegate() {
        AdminRefundDetailDto detail = mock(AdminRefundDetailDto.class);
        when(refundService.listRefundsForAdmin()).thenReturn(List.of());
        when(refundService.getRefundDetailForAdmin(1L)).thenReturn(detail);
        when(refundService.adminRequestCall(1L)).thenReturn(detail);
        when(refundService.adminApprove(1L)).thenReturn(detail);
        when(refundService.adminClose(1L)).thenReturn(detail);
        when(refundService.adminCompletePayout(1L)).thenReturn(detail);

        assertEquals(HttpStatus.OK, controller.getRefunds().getStatusCode());
        assertSame(detail, controller.getRefundDetail(1L).getBody());
        assertSame(detail, controller.requestCall(1L).getBody());
        assertSame(detail, controller.approveRefund(1L).getBody());
        assertSame(detail, controller.closeRefund(1L).getBody());
        assertSame(detail, controller.completeRefund(1L).getBody());
    }

    @Test
    void questionEndpointsDelegate() {
        Question question = new Question();
        QuestionRequest request = new QuestionRequest("Q?", QuestionType.TEXT, "general", true, true, List.of());
        when(questionService.findAll()).thenReturn(List.of(question));
        when(questionService.create(request)).thenReturn(question);
        when(questionService.update(4L, request)).thenReturn(question);

        assertEquals(1, controller.getAllQuestions().getBody().size());
        assertSame(question, controller.createQuestion(request).getBody());
        assertSame(question, controller.updateQuestion(4L, request).getBody());
        assertEquals("Question deleted", controller.deleteQuestion(4L).getBody().get("message"));
        verify(questionService).delete(4L);
    }
}
