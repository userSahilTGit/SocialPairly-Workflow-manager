package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.DeleteAccountRequest;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserAnswerRepository answerRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       UserAnswerRepository answerRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.answerRepository = answerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void deleteAccount(User currentUser, DeleteAccountRequest request) {
        String identifier = request.identifier().trim();
        User user = userRepository.findByEmail(identifier.toLowerCase())
                .or(() -> userRepository.findByPhoneNumber(identifier))
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!user.getId().equals(currentUser.getId())) {
            throw new BadRequestException("Credentials do not match your account");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }

        if (user.getRole().name().equals("ADMIN")) {
            throw new BadRequestException("Admin accounts cannot be deleted");
        }

        answerRepository.deleteAll(answerRepository.findByUserId(user.getId()));
        profileRepository.findByUserId(user.getId()).ifPresent(profileRepository::delete);
        userRepository.delete(user);
    }

    public User findByIdentifier(String identifier) {
        String trimmed = identifier.trim();
        return userRepository.findByEmail(trimmed.toLowerCase())
                .or(() -> userRepository.findByPhoneNumber(trimmed))
                .orElseThrow(() -> new ResourceNotFoundException("No account found for the given identifier"));
    }
}