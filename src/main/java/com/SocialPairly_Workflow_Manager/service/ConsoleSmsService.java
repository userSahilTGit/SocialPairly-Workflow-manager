package com.SocialPairly_Workflow_Manager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsService.class);

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        log.info("[SMS-CONSOLE] OTP {} would be sent to {} (phone verification uses Firebase Auth).",
                otp, phoneNumber);
    }
}
