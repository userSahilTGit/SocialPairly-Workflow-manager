package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.PaymentResponseDTO;
import com.SocialPairly_Workflow_Manager.dto.PlanDto;
import com.SocialPairly_Workflow_Manager.dto.PlanUpgradeDto;
import com.SocialPairly_Workflow_Manager.dto.PlanUpgradeSubmitRequest;
import com.SocialPairly_Workflow_Manager.service.PaymentService;
import com.SocialPairly_Workflow_Manager.service.PlanUpgradeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanUpgradeControllerTest {

    @Mock
    private PlanUpgradeService planUpgradeService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PlanUpgradeController planUpgradeController;

    @Test
    void getCurrentUpgradeShouldReturnActiveFalseWhenNone() {
        when(planUpgradeService.getCurrentUserUpgrade()).thenReturn(Optional.empty());

        ResponseEntity<?> response = planUpgradeController.getCurrentUpgrade();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(false, ((Map<?, ?>) response.getBody()).get("active"));
    }

    @Test
    void getCurrentUpgradeShouldReturnDtoWhenExists() {
        PlanUpgradeDto dto = sampleDto();
        when(planUpgradeService.getCurrentUserUpgrade()).thenReturn(Optional.of(dto));

        ResponseEntity<?> response = planUpgradeController.getCurrentUpgrade();

        assertEquals(200, response.getStatusCode().value());
        assertSame(dto, response.getBody());
    }

    @Test
    void getEligiblePlansShouldDelegate() {
        PlanDto plan = new PlanDto(
                2L, "Basic Monthly", "MONTHLY", 30, new BigDecimal("50.00"),
                "200", null, null, "desc", true, false
        );
        when(planUpgradeService.getEligibleUpgradePlans()).thenReturn(List.of(plan));

        ResponseEntity<List<PlanDto>> response = planUpgradeController.getEligiblePlans();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Basic Monthly", response.getBody().get(0).planName());
    }

    @Test
    void submitUpgradeShouldDelegate() {
        PlanUpgradeSubmitRequest request = new PlanUpgradeSubmitRequest(2L, "Need more tokens");
        PlanUpgradeDto dto = sampleDto();
        when(planUpgradeService.submitUpgradeRequest(request)).thenReturn(dto);

        ResponseEntity<PlanUpgradeDto> response = planUpgradeController.submitUpgrade(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(dto, response.getBody());
    }

    @Test
    void checkoutUpgradeShouldDelegateToPaymentService() {
        PaymentResponseDTO paymentResponse = PaymentResponseDTO.builder()
                .status("SUCCESS")
                .message("ok")
                .sessionId("cs_test")
                .sessionUrl("https://checkout.stripe.com/test")
                .build();
        when(paymentService.checkoutUpgrade(1L)).thenReturn(paymentResponse);

        ResponseEntity<PaymentResponseDTO> response = planUpgradeController.checkoutUpgrade(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("cs_test", response.getBody().getSessionId());
        verify(paymentService).checkoutUpgrade(1L);
    }

    private PlanUpgradeDto sampleDto() {
        return new PlanUpgradeDto(
                1L, "UPG-1", "Welcome Pass", "Basic Monthly", "Need more tokens",
                "Started", "Requested", 0, BigDecimal.ZERO, 100, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
