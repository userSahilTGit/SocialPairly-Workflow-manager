package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.CustomerCareRequest;
import com.SocialPairly_Workflow_Manager.service.EmailService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private SupportController supportController;

    @Test
    void contactCustomerCareSendsEmail() throws Exception {
        CustomerCareRequest request = new CustomerCareRequest(
                "Ada Lovelace",
                "ada@example.com",
                "+12025550123",
                List.of("Billing", "Technical"),
                "Cannot upgrade plan",
                "I am unable to complete checkout after selecting Premium.",
                "https://www.socialpairly.com/subscriptions"
        );

        ResponseEntity<Map<String, String>> response = supportController.contactCustomerCare(request);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().get("message").toLowerCase().contains("customer care"));
        verify(emailService).sendCustomerCareEmail(request);
    }

    @Test
    void contactCustomerCareAcceptsNullIssuesList() throws Exception {
        CustomerCareRequest request = new CustomerCareRequest(
                "Ada", "ada@example.com", null, null, "Help",
                "Something went wrong with my account access.", null
        );

        ResponseEntity<Map<String, String>> response = supportController.contactCustomerCare(request);

        assertEquals(200, response.getStatusCode().value());
        verify(emailService).sendCustomerCareEmail(request);
    }

    @Test
    void contactCustomerCareWrapsMailFailure() throws Exception {
        doThrow(new MessagingException("smtp down")).when(emailService).sendCustomerCareEmail(any());

        CustomerCareRequest request = new CustomerCareRequest(
                "Ada", "ada@example.com", null, List.of(), "Help",
                "Something went wrong with my account access.", null
        );

        assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                () -> supportController.contactCustomerCare(request));
    }

    @Test
    void contactCustomerCareWrapsSmtpAuthFailure() throws Exception {
        doThrow(new org.springframework.mail.MailAuthenticationException("Authentication failed"))
                .when(emailService).sendCustomerCareEmail(any());

        CustomerCareRequest request = new CustomerCareRequest(
                "Ada", "ada@example.com", null, List.of("Technical"), "Help",
                "Something went wrong with my account access.", null
        );

        assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                () -> supportController.contactCustomerCare(request));
    }
}
