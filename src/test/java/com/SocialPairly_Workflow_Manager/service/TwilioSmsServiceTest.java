package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwilioSmsServiceTest {

    private TwilioSmsService service;
    private MockedStatic<Twilio> twilioStatic;
    private MockedStatic<Message> messageStatic;

    @BeforeEach
    void setUp() {
        service = new TwilioSmsService();
        ReflectionTestUtils.setField(service, "defaultCountryCode", "+91");
        twilioStatic = mockStatic(Twilio.class);
        messageStatic = mockStatic(Message.class);
    }

    @AfterEach
    void tearDown() {
        if (messageStatic != null) messageStatic.close();
        if (twilioStatic != null) twilioStatic.close();
    }

    @Test
    void initRejectsMissingAccountSid() {
        ReflectionTestUtils.setField(service, "accountSid", null);
        ReflectionTestUtils.setField(service, "authToken", "tok");
        ReflectionTestUtils.setField(service, "fromNumber", "+15551212");
        assertThrows(IllegalStateException.class, () -> service.init());
    }

    @Test
    void initRejectsBlankAuthToken() {
        ReflectionTestUtils.setField(service, "accountSid", "AC123");
        ReflectionTestUtils.setField(service, "authToken", "  ");
        ReflectionTestUtils.setField(service, "fromNumber", "+15551212");
        assertThrows(IllegalStateException.class, () -> service.init());
    }

    @Test
    void initRejectsMissingFromNumber() {
        ReflectionTestUtils.setField(service, "accountSid", "AC123");
        ReflectionTestUtils.setField(service, "authToken", "tok");
        ReflectionTestUtils.setField(service, "fromNumber", null);
        assertThrows(IllegalStateException.class, () -> service.init());
    }

    @Test
    void initRejectsBlankFromNumber() {
        ReflectionTestUtils.setField(service, "accountSid", "AC123");
        ReflectionTestUtils.setField(service, "authToken", "tok");
        ReflectionTestUtils.setField(service, "fromNumber", "");
        assertThrows(IllegalStateException.class, () -> service.init());
    }

    @Test
    void initSucceedsWithCredentials() {
        ReflectionTestUtils.setField(service, "accountSid", "AC123");
        ReflectionTestUtils.setField(service, "authToken", "tok");
        ReflectionTestUtils.setField(service, "fromNumber", "+15551212");
        assertDoesNotThrow(() -> service.init());
        twilioStatic.verify(() -> Twilio.init("AC123", "tok"));
    }

    @Test
    void sendOtpRejectsInvalidPhone() {
        ReflectionTestUtils.setField(service, "fromNumber", "+15551212");
        assertThrows(BadRequestException.class, () -> service.sendOtp("bad", "1234"));
    }

    @Test
    void sendOtpSucceeds() {
        ReflectionTestUtils.setField(service, "fromNumber", " +15551212 ");
        MessageCreator creator = mock(MessageCreator.class);
        Message message = mock(Message.class);
        when(message.getSid()).thenReturn("SM1");
        when(message.getStatus()).thenReturn(Message.Status.SENT);
        when(creator.create()).thenReturn(message);
        messageStatic.when(() -> Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), anyString()))
                .thenReturn(creator);

        assertDoesNotThrow(() -> service.sendOtp("+919876543210", "1234"));
    }

    @Test
    void sendOtpWrapsProviderFailures() {
        ReflectionTestUtils.setField(service, "fromNumber", "+15551212");
        MessageCreator creator = mock(MessageCreator.class);
        when(creator.create()).thenThrow(new RuntimeException("trial"));
        messageStatic.when(() -> Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), anyString()))
                .thenReturn(creator);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.sendOtp("+919876543210", "1234"));
        assertTrue(ex.getMessage().contains("Failed to send SMS"));
    }
}
