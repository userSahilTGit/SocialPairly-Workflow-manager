package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.ProfileRequest;
import com.SocialPairly_Workflow_Manager.entity.Education;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);
    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(UserProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
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

        user.setProfile(saved);
        user.setProfileCompleted(true);
        userRepository.save(user);

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
}