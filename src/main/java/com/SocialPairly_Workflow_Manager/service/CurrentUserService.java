package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserService.class);
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            log.warn("No authenticated user found in security context");
            throw new ResourceNotFoundException("No authenticated user");
        }
        String email = authentication.getName();
        log.debug("Resolving current user for email={}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authenticated email not found in repository: {}", email);
                    return new ResourceNotFoundException("Authenticated user not found");
                });
    }
}