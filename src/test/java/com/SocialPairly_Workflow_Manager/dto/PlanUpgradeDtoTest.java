package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeAction;
import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeRequest;
import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeStatus;
import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PlanUpgradeDtoTest {

    @Test
    void planUpgradeDtoFromShouldMapAllFields() {
        User user = new User();
        user.setId(22L);
        user.setUserTokens(120);

        Payment payment = new Payment();
        payment.setId(88L);

        PlanUpgradeRequest request = baseRequest(user);
        request.setPayment(payment);
        request.setExtraToken(30);
        request.setExtraAmount(new BigDecimal("15.25"));
        request.setStatus(PlanUpgradeStatus.InProgress);
        request.setAction(PlanUpgradeAction.Approved);

        PlanUpgradeDto dto = PlanUpgradeDto.from(request, 120);

        assertEquals(1L, dto.id());
        assertEquals("UPG-1", dto.formattedUpgradeId());
        assertEquals("Welcome Pass", dto.currentPlan());
        assertEquals("Basic Monthly", dto.upgradePlan());
        assertEquals("Need more tokens", dto.reason());
        assertEquals("InProgress", dto.status());
        assertEquals("Approved", dto.action());
        assertEquals(30, dto.extraToken());
        assertEquals(new BigDecimal("15.25"), dto.extraAmount());
        assertEquals(120, dto.userTokens());
        assertEquals(88L, dto.paymentId());
        assertNotNull(dto.createdAt());
        assertNotNull(dto.updatedAt());
    }

    @Test
    void planUpgradeDtoFromShouldHandleNullStatusActionAndPayment() {
        User user = new User();
        user.setId(1L);

        PlanUpgradeRequest request = baseRequest(user);
        request.setStatus(null);
        request.setAction(null);
        request.setPayment(null);

        PlanUpgradeDto dto = PlanUpgradeDto.from(request, 50);

        assertNull(dto.status());
        assertNull(dto.action());
        assertNull(dto.paymentId());
        assertEquals(50, dto.userTokens());
    }

    @Test
    void adminPlanUpgradeListDtoFromShouldMapFields() {
        User user = new User();
        user.setId(22L);

        PlanUpgradeRequest request = baseRequest(user);
        request.setStatus(PlanUpgradeStatus.Started);
        request.setAction(PlanUpgradeAction.Requested);

        AdminPlanUpgradeListDto dto = AdminPlanUpgradeListDto.from(request);

        assertEquals(1L, dto.id());
        assertEquals("UPG-1", dto.formattedUpgradeId());
        assertEquals(22L, dto.userId());
        assertEquals("Basic Monthly", dto.upgradePlan());
        assertEquals("Started", dto.status());
        assertEquals("Requested", dto.action());
        assertNotNull(dto.submissionDate());
    }

    @Test
    void adminPlanUpgradeListDtoFromShouldFallbackWhenStatusActionNull() {
        User user = new User();
        user.setId(3L);

        PlanUpgradeRequest request = baseRequest(user);
        request.setStatus(null);
        request.setAction(null);

        AdminPlanUpgradeListDto dto = AdminPlanUpgradeListDto.from(request);

        assertEquals("—", dto.status());
        assertEquals("—", dto.action());
    }

    @Test
    void adminPlanUpgradeDetailDtoFromShouldMapUserNameAndTokens() {
        User user = new User();
        user.setId(22L);
        user.setFirstName("Sahil");
        user.setLastName("T");
        user.setEmail("sahil@example.com");
        user.setUserTokens(150);

        PlanUpgradeRequest request = baseRequest(user);
        request.setStatus(PlanUpgradeStatus.InProgress);
        request.setAction(PlanUpgradeAction.Approved);
        request.setExtraToken(40);
        request.setExtraAmount(new BigDecimal("20.00"));

        AdminPlanUpgradeDetailDto dto = AdminPlanUpgradeDetailDto.from(request);

        assertEquals(1L, dto.id());
        assertEquals("UPG-1", dto.formattedUpgradeId());
        assertEquals(22L, dto.userId());
        assertEquals("Sahil T", dto.userName());
        assertEquals("sahil@example.com", dto.userEmail());
        assertEquals("Welcome Pass", dto.currentPlan());
        assertEquals("Basic Monthly", dto.upgradePlan());
        assertEquals("Need more tokens", dto.reason());
        assertEquals(150, dto.existingTokens());
        assertEquals(40, dto.extraToken());
        assertEquals(new BigDecimal("20.00"), dto.extraAmount());
        assertEquals("InProgress", dto.status());
        assertEquals("Approved", dto.action());
        assertNotNull(dto.createdAt());
    }

    @Test
    void adminPlanUpgradeDetailDtoFromShouldFallbackToEmailWhenNameBlank() {
        User user = new User();
        user.setId(9L);
        user.setFirstName(null);
        user.setLastName("  ");
        user.setEmail("only@example.com");
        user.setUserTokens(40);

        PlanUpgradeRequest request = baseRequest(user);
        request.setStatus(null);
        request.setAction(null);

        AdminPlanUpgradeDetailDto dto = AdminPlanUpgradeDetailDto.from(request);

        assertEquals("only@example.com", dto.userName());
        assertNull(dto.status());
        assertNull(dto.action());
    }

    @Test
    void adminPlanUpgradeDetailDtoFromShouldUseFirstNameWhenLastNameNull() {
        User user = new User();
        user.setId(10L);
        user.setFirstName("Alex");
        user.setLastName(null);
        user.setEmail("alex@example.com");
        user.setUserTokens(60);

        AdminPlanUpgradeDetailDto dto = AdminPlanUpgradeDetailDto.from(baseRequest(user));

        assertEquals("Alex", dto.userName());
    }

    @Test
    void planUpgradeSubmitRequestShouldExposeFields() {
        PlanUpgradeSubmitRequest request = new PlanUpgradeSubmitRequest(5L, "Need more visibility");
        assertEquals(5L, request.upgradePlanId());
        assertEquals("Need more visibility", request.reason());
    }

    private PlanUpgradeRequest baseRequest(User user) {
        PlanUpgradeRequest request = new PlanUpgradeRequest();
        request.setId(1L);
        request.setUser(user);
        request.setCurrentPlan("Welcome Pass");
        request.setUpgradePlan("Basic Monthly");
        request.setReason("Need more tokens");
        request.setCreatedAt(LocalDateTime.of(2026, 8, 14, 12, 0));
        request.setUpdatedAt(LocalDateTime.of(2026, 8, 14, 12, 30));
        return request;
    }
}
