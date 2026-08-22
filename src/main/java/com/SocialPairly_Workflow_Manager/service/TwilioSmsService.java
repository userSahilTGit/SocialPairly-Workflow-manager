package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "twilio")
public class TwilioSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsService.class);

    @Value("${app.sms.twilio.account-sid}")
    private String accountSid;

    @Value("${app.sms.twilio.auth-token}")
    private String authToken;

    @Value("${app.sms.twilio.from-number}")
    private String fromNumber;

    @Value("${app.sms.default-country-code:+91}")
    private String defaultCountryCode;

    @PostConstruct
    void init() {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new IllegalStateException(
                    "Twilio credentials are missing (app.sms.twilio.account-sid / auth-token)");
        }
        if (fromNumber == null || fromNumber.isBlank()) {
            throw new IllegalStateException("Twilio from-number is missing (app.sms.twilio.from-number)");
        }
        Twilio.init(accountSid, authToken);
        log.info("Twilio SMS initialized (from={})", fromNumber);
    }

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        String to;
        try {
            to = PhoneNumberNormalizer.toE164(phoneNumber, defaultCountryCode);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid phone number: " + ex.getMessage());
        }
        String body = "Your SocialPairly verification code is: " + otp;
        try {
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromNumber.trim()),
                    body
            ).create();
            log.info("Twilio SMS sent sid={} to={} status={}", message.getSid(), to, message.getStatus());
        } catch (Exception ex) {
            log.error("Twilio SMS failed for {}: {}", to, ex.getMessage(), ex);
            throw new BadRequestException(
                    "Failed to send SMS verification code. If this is a Twilio trial account, verify the destination number in the Twilio console. Details: "
                            + ex.getMessage());
        }
    }
}
