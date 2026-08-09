package com.SocialPairly_Workflow_Manager.service;

/**
 * Abstraction for outbound SMS. Phone verification now uses Firebase Auth on the client;
 * this interface remains for optional non-verification SMS use cases.
 */
public interface SmsService {
    void sendOtp(String phoneNumber, String otp);
}
