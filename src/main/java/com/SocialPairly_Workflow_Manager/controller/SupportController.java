package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.CustomerCareRequest;
import com.SocialPairly_Workflow_Manager.dto.SupportAttachment;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.service.EmailService;
import com.SocialPairly_Workflow_Manager.service.SubscriptionService;
import com.SocialPairly_Workflow_Manager.util.LogMasker;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private static final Logger log = LoggerFactory.getLogger(SupportController.class);
    private static final int MAX_ATTACHMENTS = 3;
    private static final long MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "application/pdf");

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public SupportController(EmailService emailService) {
        this(emailService, null, null);
    }

    @Autowired
    public SupportController(
            EmailService emailService,
            UserRepository userRepository,
            SubscriptionService subscriptionService
    ) {
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Public endpoint: member (or guest) sends an issue email to customer care.
     */
    @PostMapping(value = "/contact", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> contactCustomerCare(@Valid @RequestBody CustomerCareRequest request) {
        return deliver(request, List.of());
    }

    @PostMapping(value = "/contact", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> contactCustomerCareWithAttachments(
            @RequestPart("payload") @Valid CustomerCareRequest request,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments
    ) {
        return deliver(request, toAttachments(attachments));
    }

    private ResponseEntity<Map<String, String>> deliver(CustomerCareRequest request, List<SupportAttachment> attachments) {
        boolean premiumMember = isPremiumRequester(request);
        log.info("Customer-care contact from email={} premium={} issues={} attachments={}",
                LogMasker.email(request.email()),
                premiumMember,
                request.issues() == null ? 0 : request.issues().size(),
                attachments.size());
        try {
            if (premiumMember) {
                emailService.sendCustomerCareEmail(request, attachments, true);
            } else if (attachments.isEmpty()) {
                emailService.sendCustomerCareEmail(request);
            } else {
                emailService.sendCustomerCareEmail(request, attachments);
            }
        } catch (MessagingException | MailException ex) {
            log.warn("Customer-care send failed: {}", ex.getMessage());
            throw new BadRequestException("Unable to send your message right now. Please try again shortly.");
        }
        return ResponseEntity.ok(Map.of(
                "message", "Thanks — your message was sent to customer care. We will reply by email."
        ));
    }

    private boolean isPremiumRequester(CustomerCareRequest request) {
        if (userRepository == null || subscriptionService == null) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            User authed = userRepository.findByEmail(auth.getName().trim().toLowerCase(Locale.ROOT)).orElse(null);
            if (authed != null) {
                return subscriptionService.hasActiveSubscription(authed.getId());
            }
        }
        if (request != null && request.email() != null && !request.email().isBlank()) {
            User byEmail = userRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
            if (byEmail != null) {
                return subscriptionService.hasActiveSubscription(byEmail.getId());
            }
        }
        return false;
    }

    private static List<SupportAttachment> toAttachments(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<MultipartFile> present = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (present.size() > MAX_ATTACHMENTS) {
            throw new BadRequestException("You can attach up to 3 files.");
        }
        List<SupportAttachment> attachments = new ArrayList<>();
        int index = 1;
        for (MultipartFile file : present) {
            String contentType = normalizeContentType(file.getContentType());
            String filename = safeFilename(file.getOriginalFilename(), index);
            if (!ALLOWED_TYPES.contains(contentType) || !extensionMatches(filename, contentType)) {
                throw new BadRequestException("Attachments must be PNG, JPG, or PDF.");
            }
            if (file.getSize() > MAX_ATTACHMENT_BYTES) {
                throw new BadRequestException("Each attachment must be 10MB or smaller.");
            }
            try {
                attachments.add(new SupportAttachment(filename, contentType, file.getBytes()));
            } catch (IOException ex) {
                throw new BadRequestException("Unable to read an attachment. Please try again.");
            }
            index++;
        }
        return attachments;
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).trim();
        if ("image/jpg".equals(normalized) || "image/pjpeg".equals(normalized)) {
            return "image/jpeg";
        }
        return normalized;
    }

    private static boolean extensionMatches(String filename, String contentType) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return switch (contentType) {
            case "image/png" -> lower.endsWith(".png");
            case "image/jpeg" -> lower.endsWith(".jpg") || lower.endsWith(".jpeg");
            case "application/pdf" -> lower.endsWith(".pdf");
            default -> false;
        };
    }

    private static String safeFilename(String original, int index) {
        String name = original == null ? "" : original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            name = "attachment-" + index;
        }
        if (name.length() > 120) {
            name = name.substring(name.length() - 120);
        }
        return name;
    }
}
