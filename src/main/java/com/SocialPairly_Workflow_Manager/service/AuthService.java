package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.LoginRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final UserService userService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       OtpService otpService,
                       UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.userService = userService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new BadRequestException("Phone number is already registered");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email().toLowerCase().trim());
        user.setPhoneNumber(request.phoneNumber().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setAddress(request.address());
        user.setRole(Role.USER);
        user.setProfileCompleted(false);

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getEmail());
        return new AuthResponse(token, UserDto.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = userRepository.findByEmail(identifier.toLowerCase())
                .or(() -> userRepository.findByPhoneNumber(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("No account found for the given identifier"));

        // Authenticate by email (the UserDetails username) + raw password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.password()));

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, UserDto.from(user));
    }

    public Map<String, String> sendForgotPasswordOtp(ForgotPasswordSendOtpRequest request) {
        User user = userService.findByIdentifier(request.identifier());
        String otp = otpService.generateAndStore(request.identifier());
        String destination = user.getEmail().contains("@")
                ? user.getEmail()
                : user.getPhoneNumber();
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