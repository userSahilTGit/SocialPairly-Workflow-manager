package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.OnboardingSteps;
import com.SocialPairly_Workflow_Manager.constants.ReligionOptions;
import com.SocialPairly_Workflow_Manager.dto.PersonalityLifestyleRequest;
import com.SocialPairly_Workflow_Manager.dto.PersonalityLifestyleResponse;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserPersonalityProfile;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.UserPersonalityProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PersonalityOnboardingService {

    public static final String ACTION_SAVE_LATER = "SAVE_LATER";
    public static final String ACTION_CONTINUE = "CONTINUE";
    public static final int MAX_ABOUT = 500;
    public static final int MAX_FRIEND_WORDS = 3;

    private final UserPersonalityProfileRepository personalityRepository;
    private final UserProfileRepository profileRepository;

    public PersonalityOnboardingService(
            UserPersonalityProfileRepository personalityRepository,
            UserProfileRepository profileRepository
    ) {
        this.personalityRepository = personalityRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public PersonalityLifestyleResponse getPersonality(User user) {
        // Do not INSERT on read — read-only transactions forbid writes.
        UserPersonalityProfile row = personalityRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserPersonalityProfile p = new UserPersonalityProfile();
                    p.setUser(user);
                    return p;
                });
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setUser(user);
                    return p;
                });
        return toResponse(row, profile);
    }

    @Transactional
    public PersonalityLifestyleResponse savePersonality(User user, PersonalityLifestyleRequest request) {
        String action = request.action() == null ? "" : request.action().trim().toUpperCase(Locale.ROOT);
        if (!ACTION_SAVE_LATER.equals(action) && !ACTION_CONTINUE.equals(action)) {
            throw new BadRequestException("action must be SAVE_LATER or CONTINUE");
        }
        boolean strict = ACTION_CONTINUE.equals(action);

        UserPersonalityProfile row = ensurePersonality(user);
        applyFields(row, request, strict);
        personalityRepository.save(row);

        UserProfile profile = ensureProfile(user);
        applyReligion(profile, request);
        // Keep browsing profile bio in sync with About You
        if (row.getAboutStory() != null) {
            profile.setAboutMe(row.getAboutStory());
        }
        if (strict) {
            if (isBlank(row.getHeadline()) || isBlank(row.getAboutStory())) {
                throw new BadRequestException("Headline and about story are required to continue");
            }
            profile.setOnboardingStep(OnboardingSteps.PERSONALITY_COMPLETED);
        } else if (!OnboardingSteps.PERSONALITY_COMPLETED.equals(profile.getOnboardingStep())) {
            profile.setOnboardingStep(OnboardingSteps.PERSONALITY_IN_PROGRESS);
        }
        profileRepository.save(profile);
        return toResponse(row, profile);
    }

    private void applyFields(UserPersonalityProfile row, PersonalityLifestyleRequest request, boolean strict) {
        String headline = trimToNull(request.headline());
        String about = trimToNull(request.aboutStory());
        if (about != null && about.length() > MAX_ABOUT) {
            throw new BadRequestException("About story must be at most " + MAX_ABOUT + " characters");
        }
        if (headline != null && headline.length() > 200) {
            throw new BadRequestException("Headline is too long");
        }
        if (strict && (headline == null || about == null)) {
            throw new BadRequestException("Headline and about story are required to continue");
        }

        List<String> friends = normalizeList(request.friendDescriptors(), MAX_FRIEND_WORDS, 40);
        Set<String> traits = new HashSet<>(normalizeList(request.personalityTraits(), 20, 40));

        row.setHeadline(headline);
        row.setAboutStory(about);
        row.setFriendDescriptors(friends);
        row.setProudOf(trimToNull(request.proudOf()));
        row.setLifePhilosophy(trimToNull(request.lifePhilosophy()));
        row.setPersonalityTraits(traits);
        row.setInterestsHobbies(trimToNull(request.interestsHobbies()));
        row.setLifestyleNotes(trimToNull(request.lifestyleNotes()));
        row.setRelationshipGoals(trimToNull(request.relationshipGoals()));
        row.setFirstDatePrefs(trimToNull(request.firstDatePrefs()));
        row.setIdealPartner(trimToNull(request.idealPartner()));
        row.setExtendedFamily(trimToNull(request.extendedFamily()));
    }

    private void applyReligion(UserProfile profile, PersonalityLifestyleRequest request) {
        profile.setReligion(normalizeReligion(request.religion(), false));
        profile.setPreferredReligion(normalizeReligion(request.preferredReligion(), true));
    }

    private static String normalizeReligion(String raw, boolean preferred) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        if (preferred) {
            if (!ReligionOptions.PREFERRED_RELIGION_SET.contains(value)) {
                throw new BadRequestException("Invalid preferred religion");
            }
        } else if (!ReligionOptions.RELIGION_SET.contains(value)) {
            throw new BadRequestException("Invalid religion");
        }
        return value;
    }

    private List<String> normalizeList(List<String> raw, int maxItems, int maxLen) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String item : raw) {
            String t = trimToNull(item);
            if (t == null) continue;
            if (t.length() > maxLen) {
                throw new BadRequestException("Tag exceeds maximum length");
            }
            if (!out.contains(t)) {
                out.add(t);
            }
            if (out.size() >= maxItems) break;
        }
        return out;
    }

    private UserPersonalityProfile ensurePersonality(User user) {
        return personalityRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserPersonalityProfile p = new UserPersonalityProfile();
            p.setUser(user);
            return personalityRepository.save(p);
        });
    }

    private UserProfile ensureProfile(User user) {
        return profileRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setUser(user);
            return profileRepository.save(p);
        });
    }

    private PersonalityLifestyleResponse toResponse(UserPersonalityProfile row, UserProfile profile) {
        boolean complete = OnboardingSteps.PERSONALITY_COMPLETED.equals(profile.getOnboardingStep());
        return new PersonalityLifestyleResponse(
                row.getHeadline(),
                row.getAboutStory(),
                List.copyOf(row.getFriendDescriptors()),
                row.getProudOf(),
                row.getLifePhilosophy(),
                List.copyOf(new ArrayList<>(row.getPersonalityTraits())),
                row.getInterestsHobbies(),
                row.getLifestyleNotes(),
                row.getRelationshipGoals(),
                row.getFirstDatePrefs(),
                row.getIdealPartner(),
                row.getExtendedFamily(),
                profile.getReligion(),
                profile.getPreferredReligion(),
                ReligionOptions.RELIGIONS,
                ReligionOptions.PREFERRED_RELIGIONS,
                profile.getOnboardingStep(),
                complete
        );
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
