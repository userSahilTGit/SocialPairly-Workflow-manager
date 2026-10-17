package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.CustomerCareRequest;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.service.EmailService;
import com.SocialPairly_Workflow_Manager.util.LogMasker;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private static final Logger log = LoggerFactory.getLogger(SupportController.class);

    private final EmailService emailService;

    public SupportController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Public endpoint: member (or guest) sends an issue email to customer care.
     */
    @PostMapping("/contact")
    public ResponseEntity<Map<String, String>> contactCustomerCare(@Valid @RequestBody CustomerCareRequest request) {
        log.info("Customer-care contact from email={} issues={}",
                LogMasker.email(request.email()),
                request.issues() == null ? 0 : request.issues().size());
        try {
            emailService.sendCustomerCareEmail(request);
        } catch (MessagingException | MailException ex) {
            log.warn("Customer-care send failed: {}", ex.getMessage());
            throw new BadRequestException("Unable to send your message right now. Please try again shortly.");
        }
        return ResponseEntity.ok(Map.of(
                "message", "Thanks — your message was sent to customer care. We will reply by email."
        ));
    }
}
