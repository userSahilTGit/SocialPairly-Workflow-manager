package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.RefundAction;
import com.SocialPairly_Workflow_Manager.entity.RefundStatus;
import com.SocialPairly_Workflow_Manager.service.RefundService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundControllerTest {

    @Mock
    private RefundService refundService;

    @InjectMocks
    private RefundController refundController;

    @Test
    void getCurrentRefundShouldReturnActiveFalseWhenNoRefund() {
        when(refundService.getCurrentUserRefund()).thenReturn(Optional.empty());

        ResponseEntity<?> response = refundController.getCurrentRefund();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(false, ((Map<?, ?>) response.getBody()).get("active"));
    }

    @Test
    void getCurrentRefundShouldReturnRefundDtoWhenExists() {
        RefundDto dto = new RefundDto(
                1L, "REF-1", "Initiated", "Requested", "reason",
                new BigDecimal("100.00"), "usd", null, LocalDateTime.now(), null
        );
        when(refundService.getCurrentUserRefund()).thenReturn(Optional.of(dto));

        ResponseEntity<?> response = refundController.getCurrentRefund();

        assertEquals(200, response.getStatusCode().value());
        assertSame(dto, response.getBody());
    }

    @Test
    void submitRefundShouldDelegateToService() {
        RefundRequestDto request = new RefundRequestDto("reason");
        RefundDto dto = new RefundDto(
                2L, "REF-2", "Initiated", "Requested", "reason",
                new BigDecimal("50.00"), "usd", null, LocalDateTime.now(), null
        );
        when(refundService.submitRefundRequest(request)).thenReturn(dto);

        ResponseEntity<RefundDto> response = refundController.submitRefund(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(dto, response.getBody());
    }

    @Test
    void submitSlotShouldDelegateToService() {
        SlotRequestDto request = new SlotRequestDto(LocalDateTime.now().plusDays(1));
        RefundDto dto = new RefundDto(
                3L, "REF-3", "In-Progress", "Provided Slot", "reason",
                new BigDecimal("50.00"), "usd", request.slot(), LocalDateTime.now(), null
        );
        when(refundService.submitSlot(3L, request)).thenReturn(dto);

        ResponseEntity<RefundDto> response = refundController.submitSlot(3L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Provided Slot", response.getBody().action());
    }

    @Test
    void submitBankDetailsShouldDelegateToService() {
        BankDetailsRequestDto request = new BankDetailsRequestDto(
                "John", "Chase", "123", "Checking", "021000021", "123 Main"
        );
        RefundDto dto = new RefundDto(
                4L, "REF-4", "In-Progress", "Provided Bank Details", "reason",
                new BigDecimal("50.00"), "usd", null, LocalDateTime.now(), null
        );
        when(refundService.submitBankDetails(4L, request)).thenReturn(dto);

        ResponseEntity<RefundDto> response = refundController.submitBankDetails(4L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Provided Bank Details", response.getBody().action());
    }
}
