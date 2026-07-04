package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    // Dependency injections (userRepository, userService, otpService, jwtUtil, authenticationManager, passwordEncoder) are omitted in the visible snippets.

    public AuthResponse login(LoginRequest request) {
        // Authenticate by email (the UserDetails username) + raw password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.password()));

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, UserDto.from(user));
    }

    public Map<String, String> sendForgotPasswordOtp(ForgotPasswordSendOtpRequest request) {
        User user = userService.findByIdentifier(request.identifier());
        String otp = otpService.generateAndStore(request.identifier());
        String destination = user.getEmail().contains("@") ? user.getEmail() : user.getPhoneNumber();
        
        log.info("Password reset OTP sent to {} for user {}", destination, user.getEmail());
        return Map.of("message", "OTP sent to your registered email or phone number");
    }

    public Map<String, String> verifyForgotPasswordOtp(ForgotPasswordVerifyOtpRequest request) {
        userService.findByIdentifier(request.identifier());
        otpService.verify(request.identifier(), request.otp());
        return Map.of("message", "OTP verified successfully");
    }

    @Transactional
    public Map<String, String> resetPassword(ForgotPasswordResetRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        
        otpService.assertVerified(request.identifier(), request.otp());
        User user = userService.findByIdentifier(request.identifier());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        otpService.clear(request.identifier());
        
        return Map.of("message", "Password reset successfully");
    }
}