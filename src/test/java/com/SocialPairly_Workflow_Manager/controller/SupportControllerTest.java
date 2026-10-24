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
