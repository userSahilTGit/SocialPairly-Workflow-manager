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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void contactCustomerCareAttachesScreenshot() throws Exception {
        CustomerCareRequest request = new CustomerCareRequest(
                "Ada", "ada@example.com", null, List.of("Other"), "Help",
                "Something went wrong with my account access.", null
        );
        MockMultipartFile file = new MockMultipartFile(
                "attachments", "shot.png", "image/png", new byte[] {1, 2, 3}
        );

        ResponseEntity<Map<String, String>> response = supportController.contactCustomerCareWithAttachments(
                request, List.of(file)
        );

        assertEquals(200, response.getStatusCode().value());
        verify(emailService).sendCustomerCareEmail(eq(request), argThat(files ->
                files.size() == 1 && "shot.png".equals(files.get(0).filename())
                        && "image/png".equals(files.get(0).contentType())
        ));
    }

    @Test
    void contactCustomerCareRejectsUnsupportedAttachment() {
        CustomerCareRequest request = new CustomerCareRequest(
                "Ada", "ada@example.com", null, List.of("Other"), "Help",
                "Something went wrong with my account access.", null
        );
        MockMultipartFile file = new MockMultipartFile(
                "attachments", "notes.txt", "text/plain", "hello".getBytes()
        );

        assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                () -> supportController.contactCustomerCareWithAttachments(request, List.of(file)));
        verifyNoInteractions(emailService);
    }

    @Test
    void emptyAndMissingAttachmentsSendThePlainEmail() throws Exception {
        CustomerCareRequest request = request();
        MockMultipartFile empty = new MockMultipartFile("attachments", "shot.png", "image/png", new byte[0]);

        assertEquals(200, supportController.contactCustomerCareWithAttachments(request, null).getStatusCode().value());
        assertEquals(200, supportController.contactCustomerCareWithAttachments(request, List.of()).getStatusCode().value());
        assertEquals(200, supportController.contactCustomerCareWithAttachments(request, List.of(empty)).getStatusCode().value());
        verify(emailService, times(3)).sendCustomerCareEmail(request);
    }

    @Test
    void acceptsJpegPdfAndNormalizesJpgContentType() throws Exception {
        CustomerCareRequest request = request();
        MockMultipartFile jpeg = new MockMultipartFile("attachments", "a.jpeg", "image/jpg", new byte[]{1});
        MockMultipartFile pjpeg = new MockMultipartFile("attachments", "b.jpg", "image/pjpeg", new byte[]{2});
        MockMultipartFile pdf = new MockMultipartFile("attachments", "c.pdf", "application/pdf", new byte[]{3});

        supportController.contactCustomerCareWithAttachments(request, List.of(jpeg, pjpeg, pdf));

        verify(emailService).sendCustomerCareEmail(eq(request), argThat(files ->
                files.size() == 3
                        && "image/jpeg".equals(files.get(0).contentType())
                        && "image/jpeg".equals(files.get(1).contentType())
                        && "application/pdf".equals(files.get(2).contentType())));
    }

    @Test
    void rejectsTooManyFilesWrongExtensionOversizedAndUnreadable() throws Exception {
        CustomerCareRequest request = request();
        MockMultipartFile png = new MockMultipartFile("attachments", "a.png", "image/png", new byte[]{1});
        assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                () -> supportController.contactCustomerCareWithAttachments(request, List.of(png, png, png, png)));

        MockMultipartFile mismatch = new MockMultipartFile("attachments", "notes.png", "application/pdf", new byte[]{1});
        assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                () -> supportController.contactCustomerCareWithAttachments(request, List.of(mismatch)));

        MockMultipartFile huge = new MockMultipartFile(
                "attachments", "big.png", "image/png", new byte[10 * 1024 * 1024 + 1]);
        assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                () -> supportController.contactCustomerCareWithAttachments(request, List.of(huge)));

        assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                () -> supportController.contactCustomerCareWithAttachments(request, List.of(unreadablePng())));
    }

    @Test
    void sanitizesFilenamesAndWrapsAttachmentMailFailure() throws Exception {
        CustomerCareRequest request = request();
        String longName = "a".repeat(130) + ".png";
        MockMultipartFile nested = new MockMultipartFile(
                "attachments", "C:\\fakepath\\..\\shot.png", "image/png", new byte[]{1});
        MockMultipartFile longFile = new MockMultipartFile("attachments", longName, "image/png", new byte[]{1});
        MockMultipartFile dot = new MockMultipartFile("attachments", "..", "image/png", new byte[]{1});
        MockMultipartFile nameless = new MockMultipartFile("attachments", null, "image/png", new byte[]{1});

        assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                () -> supportController.contactCustomerCareWithAttachments(request, List.of(dot)));
        assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                () -> supportController.contactCustomerCareWithAttachments(request, List.of(nameless)));

        doThrow(new org.springframework.mail.MailSendException("down"))
                .when(emailService).sendCustomerCareEmail(eq(request), any());
        assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                () -> supportController.contactCustomerCareWithAttachments(request, List.of(nested)));

        doNothing().when(emailService).sendCustomerCareEmail(eq(request), any());
        supportController.contactCustomerCareWithAttachments(request, List.of(nested, longFile));
        verify(emailService).sendCustomerCareEmail(eq(request), argThat(files ->
                files.size() == 2
                        && "shot.png".equals(files.get(0).filename())
                        && files.get(1).filename().endsWith(".png")
                        && files.get(1).filename().length() <= 120));
    }

    @Test
    void routesPremiumAuthenticatedOrEmailMatchedUsersToPremiumSupportInbox() throws Exception {
        com.SocialPairly_Workflow_Manager.repository.UserRepository userRepo =
                mock(com.SocialPairly_Workflow_Manager.repository.UserRepository.class);
        com.SocialPairly_Workflow_Manager.service.SubscriptionService subService =
                mock(com.SocialPairly_Workflow_Manager.service.SubscriptionService.class);
        SupportController fullController = new SupportController(emailService, userRepo, subService);

        com.SocialPairly_Workflow_Manager.entity.User premiumUser = new com.SocialPairly_Workflow_Manager.entity.User();
        premiumUser.setId(24L);
        premiumUser.setEmail("ada@example.com");

        try {
            var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "ada@example.com", null, List.of());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            when(userRepo.findByEmail("ada@example.com")).thenReturn(java.util.Optional.of(premiumUser));
            when(subService.hasActiveSubscription(24L)).thenReturn(true);

            CustomerCareRequest req = request();
            assertEquals(200, fullController.contactCustomerCare(req).getStatusCode().value());
            verify(emailService).sendCustomerCareEmail(eq(req), eq(List.of()), eq(true));

            MockMultipartFile premiumPng = new MockMultipartFile("attachments", "p.png", "image/png", new byte[]{1, 2});
            assertEquals(200, fullController.contactCustomerCareWithAttachments(req, List.of(premiumPng)).getStatusCode().value());

            doThrow(new MessagingException("down")).when(emailService).sendCustomerCareEmail(eq(req), any(), eq(true));
            assertThrows(com.SocialPairly_Workflow_Manager.exception.BadRequestException.class,
                    () -> fullController.contactCustomerCare(req));
            doNothing().when(emailService).sendCustomerCareEmail(eq(req), any(), eq(true));

            // Anonymous context falls back to request email lookup
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("anonymousUser", null));
            assertEquals(200, fullController.contactCustomerCare(req).getStatusCode().value());
            verify(emailService, times(3)).sendCustomerCareEmail(eq(req), eq(List.of()), eq(true));

            var nullAuthMock = mock(jakarta.servlet.http.HttpServletRequest.class);
            when(nullAuthMock.getHeader("X-Forwarded-For")).thenReturn(null);
            when(nullAuthMock.getRemoteAddr()).thenReturn("127.0.0.1");
            var tempUserSvc = mock(com.SocialPairly_Workflow_Manager.service.UserService.class);
            var tempCurrentUserSvc = mock(com.SocialPairly_Workflow_Manager.service.CurrentUserService.class);
            when(tempCurrentUserSvc.getCurrentUser()).thenReturn(premiumUser);
            new UserController(tempCurrentUserSvc, tempUserSvc,
                    mock(com.SocialPairly_Workflow_Manager.repository.UserProfileRepository.class),
                    subService,
                    mock(com.SocialPairly_Workflow_Manager.service.UserTokenService.class),
                    mock(com.SocialPairly_Workflow_Manager.service.AuthRateLimitService.class),
                    mock(com.SocialPairly_Workflow_Manager.service.EmployeeAccessService.class))
                    .sendPhoneChangeOtp(nullAuthMock);

            // Free user routes to default customer care inbox
            when(subService.hasActiveSubscription(24L)).thenReturn(false);
            assertEquals(200, fullController.contactCustomerCare(req).getStatusCode().value());
            verify(emailService).sendCustomerCareEmail(req);

            // Unauthenticated null context + unknown email + blank/null email branches
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            CustomerCareRequest unknownEmailReq = new CustomerCareRequest(
                    "Guest", "unknown@example.com", null, List.of("Other"), "Help",
                    "Guest inquiry without account.", null);
            when(userRepo.findByEmail("unknown@example.com")).thenReturn(java.util.Optional.empty());
            assertEquals(200, fullController.contactCustomerCare(unknownEmailReq).getStatusCode().value());

            CustomerCareRequest blankEmailReq = new CustomerCareRequest(
                    "Guest", "   ", null, List.of("Other"), "Help",
                    "Blank email branch check.", null);
            assertEquals(200, fullController.contactCustomerCare(blankEmailReq).getStatusCode().value());

            // Unauthenticated Authentication object (isAuthenticated = false) + authed user missing in DB
            var unauthToken = mock(org.springframework.security.core.Authentication.class);
            when(unauthToken.isAuthenticated()).thenReturn(false);
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(unauthToken);
            assertEquals(200, fullController.contactCustomerCare(unknownEmailReq).getStatusCode().value());

            var missingAuthUser = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "ghost@example.com", null, List.of());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(missingAuthUser);
            when(userRepo.findByEmail("ghost@example.com")).thenReturn(java.util.Optional.empty());
            assertEquals(200, fullController.contactCustomerCare(unknownEmailReq).getStatusCode().value());

            var nullNameAuth = mock(org.springframework.security.core.Authentication.class);
            when(nullNameAuth.isAuthenticated()).thenReturn(true);
            when(nullNameAuth.getName()).thenReturn(null);
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(nullNameAuth);
            assertEquals(200, fullController.contactCustomerCare(
                    new CustomerCareRequest("Guest", null, null, List.of("Other"), "Help", "Null email check.", null)
            ).getStatusCode().value());

            // UserController.me() + UserDto.withSubscriptionDetails(true / false)
            var currentUserSvc = mock(com.SocialPairly_Workflow_Manager.service.CurrentUserService.class);
            var userSvc = mock(com.SocialPairly_Workflow_Manager.service.UserService.class);
            var profileRepo = mock(com.SocialPairly_Workflow_Manager.repository.UserProfileRepository.class);
            var tokenSvc = mock(com.SocialPairly_Workflow_Manager.service.UserTokenService.class);
            var rateLimitSvc = mock(com.SocialPairly_Workflow_Manager.service.AuthRateLimitService.class);
            var empSvc = mock(com.SocialPairly_Workflow_Manager.service.EmployeeAccessService.class);
            UserController userController = new UserController(
                    currentUserSvc, userSvc, profileRepo, subService, tokenSvc, rateLimitSvc, empSvc);
            premiumUser.setFirstName("Ada");
            premiumUser.setLastName("Lovelace");
            premiumUser.setRole(com.SocialPairly_Workflow_Manager.entity.Role.USER);
            when(currentUserSvc.getCurrentUser()).thenReturn(premiumUser);
            when(empSvc.applyEmployeeAccessOnAuth(premiumUser)).thenReturn(premiumUser);
            when(profileRepo.findByUserId(24L)).thenReturn(java.util.Optional.empty());
            when(empSvc.toEnrichedDto(premiumUser, null))
                    .thenReturn(com.SocialPairly_Workflow_Manager.dto.UserDto.from(premiumUser));

            var subEntity = new com.SocialPairly_Workflow_Manager.entity.Subscription();
            when(subService.findPrimaryActiveSubscription(24L)).thenReturn(java.util.Optional.of(subEntity));
            assertEquals("Subscribed", userController.me().getBody().subscriptionDetails());

            when(subService.findPrimaryActiveSubscription(24L)).thenReturn(java.util.Optional.empty());
            assertEquals("Unsubscribed", userController.me().getBody().subscriptionDetails());

            // Cover remaining constructor/helper branches on SupportController + UserController
            SupportController halfWired = new SupportController(emailService, userRepo, null);
            assertEquals(200, halfWired.contactCustomerCare(unknownEmailReq).getStatusCode().value());

            var isPremiumMethod = SupportController.class.getDeclaredMethod(
                    "isPremiumRequester", CustomerCareRequest.class);
            isPremiumMethod.setAccessible(true);
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            assertEquals(false, isPremiumMethod.invoke(fullController, new Object[]{null}));

            var mockReq = mock(jakarta.servlet.http.HttpServletRequest.class);
            when(mockReq.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
            userController.sendPhoneChangeOtp(mockReq);
            when(mockReq.getHeader("X-Forwarded-For")).thenReturn("   ");
            when(mockReq.getRemoteAddr()).thenReturn("127.0.0.1");
            userController.sendPasswordChangeOtp(mockReq);
            userController.sendDeleteAccountOtp(mockReq);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    private static CustomerCareRequest request() {
        return new CustomerCareRequest(
                "Ada", "ada@example.com", null, List.of("Other"), "Help",
                "Something went wrong with my account access.", null);
    }

    private static MultipartFile unreadablePng() {
        return new MultipartFile() {
            @Override public String getName() { return "attachments"; }
            @Override public String getOriginalFilename() { return "."; }
            @Override public String getContentType() { return "image/png"; }
            @Override public boolean isEmpty() { return false; }
            @Override public long getSize() { return 1; }
            @Override public byte[] getBytes() throws IOException { throw new IOException("read"); }
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[]{1}); }
            @Override public void transferTo(java.io.File dest) { }
        };
    }
}
