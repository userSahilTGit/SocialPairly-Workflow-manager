package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.ReligionOptions;
import com.SocialPairly_Workflow_Manager.dto.ProfileRequest;
import com.SocialPairly_Workflow_Manager.entity.Education;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);
    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ProfileService(UserProfileRepository profileRepository, UserRepository userRepository, EmailService emailService) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public UserProfile getProfile(User user) {
        log.debug("Fetching profile for userId={}", user.getId());
        return profileRepository.findByUserId(user.getId()).orElse(null);
    }

    @Transactional
    public UserProfile updateProfile(User user, ProfileRequest request) {
        log.info("Updating profile for userId={}", user.getId());
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setUser(user);
                    return p;
                });
        profile.setUser(user);

        profile.setAboutMe(request.aboutMe());
        profile.setOccupation(request.occupation());
        profile.setLifestyle(request.lifestyle());
        profile.setLocationCity(request.locationCity());
        profile.setLocationCountry(request.locationCountry());
        profile.setLatitude(request.latitude());
        profile.setLongitude(request.longitude());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender());
        profile.setReligion(normalizeReligion(request.religion(), false));
        profile.setPreferredReligion(normalizeReligion(request.preferredReligion(), true));
        profile.setInterests(request.interests() == null ? new HashSet<>() : new HashSet<>(request.interests()));

        // Rebuild educations
        profile.getEducations().clear();
        if (request.educations() != null) {
            for (ProfileRequest.EducationDto e : request.educations()) {
                if (e.institution() == null || e.institution().isBlank()) {
                    continue;
                }
                Education edu = new Education();
                edu.setProfile(profile);
                edu.setInstitution(e.institution());
                edu.setDegree(e.degree());
                edu.setFieldOfStudy(e.fieldOfStudy());
                edu.setStartYear(e.startYear());
                edu.setEndYear(e.endYear());
                profile.getEducations().add(edu);
            }
        }

        UserProfile saved = profileRepository.save(profile);

        boolean wasProfileCompleted = user.isProfileCompleted();
        user.setProfile(saved);
        user.setProfileCompleted(true);
        userRepository.save(user);

        if (!wasProfileCompleted) {
            emailService.sendProfileCompletedEmail(user);
        }

        return saved;
    }

    @Transactional
    public UserProfile setProfilePhoto(User user, String photoUrl) {
        log.info("Updating profile photo for userId={} url={}", user.getId(), photoUrl);
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setUser(user);
                    return p;
                });
        profile.setUser(user);
        profile.setProfilePhotoUrl(photoUrl);
        UserProfile saved = profileRepository.save(profile);
        user.setProfile(saved);
        userRepository.save(user);
        return saved;
    }

    private static String normalizeReligion(String raw, boolean preferred) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (preferred) {
            if (!ReligionOptions.PREFERRED_RELIGION_SET.contains(value)) {
                throw new BadRequestException("Invalid preferred religion");
            }
        } else if (!ReligionOptions.RELIGION_SET.contains(value)) {
            throw new BadRequestException("Invalid religion");
        }
        return value;
    }
}