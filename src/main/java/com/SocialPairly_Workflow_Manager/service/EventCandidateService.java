package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.EventCandidateDto;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserIdentityBackground;
import com.SocialPairly_Workflow_Manager.entity.UserLifeProfile;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.repository.UserIdentityBackgroundRepository;
import com.SocialPairly_Workflow_Manager.repository.UserLifeProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EventCandidateService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserLifeProfileRepository userLifeProfileRepository;
    private final UserIdentityBackgroundRepository userIdentityBackgroundRepository;

    public EventCandidateService(UserRepository userRepository,
                                 UserProfileRepository userProfileRepository,
                                 UserLifeProfileRepository userLifeProfileRepository,
                                 UserIdentityBackgroundRepository userIdentityBackgroundRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userLifeProfileRepository = userLifeProfileRepository;
        this.userIdentityBackgroundRepository = userIdentityBackgroundRepository;
    }

    @Transactional(readOnly = true)
    public List<EventCandidateDto> searchCandidates(
            String query,
            Boolean verifiedOnly,
            String gender,
            String religion,
            Integer minAge,
            Integer maxAge,
            String location,
            String profession,
            String educationLevel,
            String maritalStatus
    ) {
        List<User> users = userRepository.findByRole(Role.USER);
        if (users.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, UserProfile> profiles = userProfileRepository.findAll().stream()
                .filter(p -> p.getUser() != null && userIds.contains(p.getUser().getId()))
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a));
        Map<Long, UserLifeProfile> lifeProfiles = userLifeProfileRepository.findAll().stream()
                .filter(lp -> lp.getUser() != null && userIds.contains(lp.getUser().getId()))
                .collect(Collectors.toMap(lp -> lp.getUser().getId(), Function.identity(), (a, b) -> a));
        Map<Long, UserIdentityBackground> identityProfiles = userIdentityBackgroundRepository.findAll().stream()
                .filter(ib -> ib.getUser() != null && userIds.contains(ib.getUser().getId()))
                .collect(Collectors.toMap(ib -> ib.getUser().getId(), Function.identity(), (a, b) -> a));

        String normalizedQuery = normalize(query);

        return users.stream()
                .filter(user -> verifiedOnly == null || !verifiedOnly || user.isVerified())
                .map(user -> EventCandidateDto.from(
                        user,
                        profiles.get(user.getId()),
                        lifeProfiles.get(user.getId()),
                        identityProfiles.get(user.getId())
                ))
                .filter(candidate -> matchesQuery(candidate, normalizedQuery))
                .filter(candidate -> matchesGender(candidate, gender))
                .filter(candidate -> matchesReligion(candidate, religion))
                .filter(candidate -> matchesAge(candidate, minAge, maxAge))
                .filter(candidate -> matchesLocation(candidate, location))
                .filter(candidate -> matchesProfession(candidate, profession))
                .filter(candidate -> matchesEducation(candidate, educationLevel))
                .filter(candidate -> matchesMaritalStatus(candidate, maritalStatus))
                .sorted((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private static boolean matchesQuery(EventCandidateDto candidate, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return contains(candidate.displayName(), query)
                || contains(candidate.email(), query)
                || String.valueOf(candidate.userId()).contains(query)
                || contains(candidate.gender(), query)
                || contains(candidate.religion(), query)
                || contains(candidate.occupation(), query)
                || contains(candidate.locationCity(), query)
                || contains(candidate.stateRegion(), query)
                || contains(candidate.maritalStatus(), query)
                || contains(candidate.educationLevel(), query);
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private static boolean matchesGender(EventCandidateDto candidate, String gender) {
        return gender == null || gender.isBlank()
                || (candidate.gender() != null && candidate.gender().equalsIgnoreCase(gender.trim()));
    }

    private static boolean matchesReligion(EventCandidateDto candidate, String religion) {
        return religion == null || religion.isBlank()
                || (candidate.religion() != null && candidate.religion().equalsIgnoreCase(religion.trim()));
    }

    private static boolean matchesAge(EventCandidateDto candidate, Integer minAge, Integer maxAge) {
        if (minAge == null && maxAge == null) {
            return true;
        }
        if (candidate.age() == null) {
            return false;
        }
        if (minAge != null && candidate.age() < minAge) {
            return false;
        }
        return maxAge == null || candidate.age() <= maxAge;
    }

    private static boolean matchesLocation(EventCandidateDto candidate, String location) {
        if (location == null || location.isBlank()) {
            return true;
        }
        String needle = location.trim().toLowerCase();
        return contains(candidate.locationCity(), needle) || contains(candidate.stateRegion(), needle);
    }

    private static boolean matchesProfession(EventCandidateDto candidate, String profession) {
        return profession == null || profession.isBlank()
                || contains(candidate.occupation(), profession.trim().toLowerCase());
    }

    private static boolean matchesEducation(EventCandidateDto candidate, String educationLevel) {
        return educationLevel == null || educationLevel.isBlank()
                || contains(candidate.educationLevel(), educationLevel.trim().toLowerCase());
    }

    private static boolean matchesMaritalStatus(EventCandidateDto candidate, String maritalStatus) {
        return maritalStatus == null || maritalStatus.isBlank()
                || (candidate.maritalStatus() != null && candidate.maritalStatus().equalsIgnoreCase(maritalStatus.trim()));
    }
}
