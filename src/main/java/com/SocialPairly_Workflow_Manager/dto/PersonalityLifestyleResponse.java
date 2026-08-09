package com.SocialPairly_Workflow_Manager.dto;

import java.util.List;

public record PersonalityLifestyleResponse(
        String headline,
        String aboutStory,
        List<String> friendDescriptors,
        String proudOf,
        String lifePhilosophy,
        List<String> personalityTraits,
        String interestsHobbies,
        String lifestyleNotes,
        String relationshipGoals,
        String firstDatePrefs,
        String idealPartner,
        String extendedFamily,
        String religion,
        String preferredReligion,
        List<String> religionOptions,
        List<String> preferredReligionOptions,
        String onboardingStep,
        boolean personalityComplete
) {}
