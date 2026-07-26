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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Service;
import java.security.SecureRandom;
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
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${spring.mail.username}")
    private String senderEmail;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       OtpService otpService,
                       UserService userService,
                       JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.userService = userService;
        this.mailSender = mailSender;
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

    @Transactional
    public AuthResponse loginOrRegisterGoogleUser(String email, String firstName, String lastName) {
        // 1. Look up user by email via your UserRepository
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseGet(() -> {
                    // 2. Fallback: Implicit Sign-Up if registration record is empty
                    User newUser = new User();
                    newUser.setEmail(email.toLowerCase().trim());
                    newUser.setFirstName(firstName);
                    newUser.setLastName(lastName);
                    newUser.setRole(Role.USER); // Default standard privilege
                    newUser.setProfileCompleted(false); // Flagging profile completion for extra fields if needed
                    newUser.setPhoneNumber(""); // Blank placeholder since Google returns no phone numbers
                    newUser.setPassword(""); // Empty password block for OAuth profiles
                    return userRepository.save(newUser);
                });

        // 3. Generate your local system's secure context token string
        String systemJwtToken = jwtUtil.generateToken(user.getEmail());

        // 4. Return matching traditional AuthResponse block format using your Record/DTO structure
        return new AuthResponse(systemJwtToken, UserDto.from(user));
    }

    public void sendForgotPasswordOtp(ForgotPasswordSendOtpRequest request) throws MessagingException {
        User user = userService.findByIdentifier(request.identifier());
        String otp = otpService.generateAndStore(request.identifier());
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        String userName = user.getFirstName() + " " + user.getLastName();
        helper.setFrom(senderEmail);
        helper.setTo(user.getEmail());
        helper.setSubject("Password Reset Verification Code");

        String htmlContent = "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px; color: #333333;'>" +
                "  <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.05);'>" +
                "    <h2 style='color: #2c3e50; border-bottom: 2px solid #eaedd; padding-bottom: 10px; margin-top: 0;'>Password Reset Request</h2>" +
                "    <p>Hi " + userName + ",</p>"+
                "    <p>We received a request to reset your account password. Please use the verification One-Time Password (OTP) below to complete your process:</p>" +
                "    <div style='text-align: center; margin: 30px 0;'>" +
                "      <span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #2e86de; background-color: #f0f7ff; padding: 10px 30px; border-radius: 5px; border: 1px dashed #2e86de; display: inline-block;'>" + otp + "</span>" +
                "    </div>" +
                "    <p style='color: #e74c3c; font-weight: bold;'>This OTP is valid for the next 10 minutes.</p>" +
                "    <p style='font-size: 13px; color: #7f8c8d; line-height: 1.5;'>" +
                "      If you did not make this request, you can safely ignore this email. Your password will remain unchanged, but you may want to review your security settings." +
                "    </p>" +
                "    <hr style='border: none; border-top: 1px solid #eeeeee; margin: 20px 0;'>" +
                "    <p style='font-size: 12px; color: #95a5a6; text-align: center; margin-bottom: 0;'>This is an automated security transmission. Please do not reply directly to this email.</p>" +
                "  </div>" +
                "</body>" +
                "</html>";

        helper.setText(htmlContent, true);
        mailSender.send(message);

        String destination = user.getEmail().contains("@")
                ? user.getEmail()
                : user.getPhoneNumber();
        log.info("Password reset OTP sent to {} for user {}", destination, user.getEmail());
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